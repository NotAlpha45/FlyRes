package com.flyres.control;

import com.flyres.common.exception.AccessDeniedException;
import com.flyres.common.exception.BookingException;
import com.flyres.common.exception.NotFoundException;
import com.flyres.domain.booking.BookingConfirmation;
import com.flyres.domain.booking.BookingRequest;
import com.flyres.domain.booking.MyTrips;
import com.flyres.domain.booking.Payment;
import com.flyres.domain.booking.Reservation;
import com.flyres.domain.booking.ReservationDetails;
import com.flyres.domain.flight.Flight;
import com.flyres.domain.flight.FlightStatus;
import com.flyres.domain.flight.Seat;
import com.flyres.domain.flight.SeatAvailability;
import com.flyres.domain.user.Role;
import com.flyres.domain.user.User;
import com.flyres.repository.FlightRepository;
import com.flyres.repository.ReservationRepository;
import com.flyres.repository.SeatRepository;
import com.flyres.data.TransactionRunner;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Random;

/**
 * &lt;&lt;control&gt;&gt; Booking a flight (fare check, simulated payment and reservation in one transaction),
 * viewing the customer's own trips, cancelling them with a full refund, and changing their seat.
 */
public final class BookingController {

    /** Shown when the chosen seat does not belong to the flight being booked. */
    public static final String SEAT_NOT_ON_FLIGHT = "That seat is not on this flight. Pick a seat from the map.";

    /** Shown when a reservation id does not exist. */
    public static final String TRIP_NOT_FOUND = "This trip was not found.";

    /** Shown when cancelling a trip that is already cancelled (e.g. from a stale screen). */
    public static final String ALREADY_CANCELLED = "This trip is already cancelled.";

    /** Shown when cancelling a trip whose flight has departed. */
    public static final String ALREADY_DEPARTED = "This trip has already departed and can't be cancelled.";

    /** Shown when the chosen new seat is the reservation's current seat. */
    public static final String SAME_SEAT = "That is already your seat. Pick a different one.";

    /** Shown when changing the seat of a cancelled trip. */
    public static final String SEAT_CHANGE_CANCELLED = "This trip is cancelled, so its seat can't be changed.";

    /** Shown when changing the seat of a trip whose flight has departed. */
    public static final String SEAT_CHANGE_DEPARTED = "This trip has already departed, so its seat can't be changed.";

    private final ReservationRepository reservations;
    private final SeatRepository seats;
    private final FlightRepository flights;
    private final PaymentController payments;
    private final TransactionRunner transactions;
    private final Clock clock;
    private final ConfirmationCodeGenerator codes;

    public BookingController(ReservationRepository reservations, SeatRepository seats, FlightRepository flights,
                             PaymentController payments, TransactionRunner transactions, Clock clock, Random random) {
        this.reservations = Objects.requireNonNull(reservations);
        this.seats = Objects.requireNonNull(seats);
        this.flights = Objects.requireNonNull(flights);
        this.payments = Objects.requireNonNull(payments);
        this.transactions = Objects.requireNonNull(transactions);
        this.clock = Objects.requireNonNull(clock);
        this.codes = new ConfirmationCodeGenerator(random);
    }

    /** Books the chosen seat and records its completed payment atomically; returns both. */
    public BookingConfirmation book(User actor, BookingRequest request) {
        Access.require(actor, "book a flight", Role.CUSTOMER);
        Objects.requireNonNull(request, "request");
        if (request.customerId() != actor.getId()) {
            throw new AccessDeniedException(
                    "You don't have permission to book for another customer.");
        }

        return transactions.inTransaction(() -> bookInTransaction(actor, request));
    }

    /** The actor's own reservations, grouped into upcoming, past and cancelled trips. */
    public MyTrips listMyReservations(User actor) {
        Access.require(actor, "view trips", Role.CUSTOMER);
        return MyTrips.of(reservations.findByCustomer(actor.getId()).stream()
                .map(reservation -> reservation.summaryAt(clock))
                .toList());
    }

    /** One of the actor's own reservations with its payment record. */
    public ReservationDetails getReservation(User actor, long reservationId) {
        Access.require(actor, "view trips", Role.CUSTOMER);
        Reservation reservation = reservations.findById(reservationId)
                .orElseThrow(() -> new NotFoundException(TRIP_NOT_FOUND));
        if (reservation.getCustomerId() != actor.getId()) {
            throw new AccessDeniedException("You can only view your own trips.");
        }
        return new ReservationDetails(reservation.summaryAt(clock), payments.paymentFor(reservationId).orElse(null));
    }

    /**
     * Cancels one of the actor's upcoming trips with a full refund of the amount paid, in one transaction:
     * the reservation becomes CANCELLED (freeing the seat) and its payment REFUNDED. Returns the trip as it
     * now stands.
     */
    public ReservationDetails cancelReservation(User actor, long reservationId) {
        Access.require(actor, "cancel a trip", Role.CUSTOMER);
        return transactions.inTransaction(() -> {
            Reservation reservation = reservations.findById(reservationId)
                    .orElseThrow(() -> new NotFoundException(TRIP_NOT_FOUND));
            if (reservation.getCustomerId() != actor.getId()) {
                throw new AccessDeniedException("You can only cancel your own trips.");
            }
            switch (reservation.statusAt(clock)) {
                case CANCELLED -> throw new BookingException(ALREADY_CANCELLED);
                case COMPLETED -> throw new BookingException(ALREADY_DEPARTED);
                default -> { }
            }

            LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
            reservation.cancel(now);
            reservations.markCancelled(reservationId, now);
            // The refund is the stored amount paid, never the flight's current price.
            Payment refunded = payments.refund(reservationId);
            return new ReservationDetails(reservation.summaryAt(clock), refunded);
        });
    }

    /**
     * Moves one of the actor's upcoming trips to a free seat on the same flight, in one transaction.
     * Modify means change seat only: the flight, amount paid, payment and confirmation code stay the same,
     * even if the flight's price has changed since booking. Returns the trip as it now stands.
     */
    public ReservationDetails changeSeat(User actor, long reservationId, long newSeatId) {
        Access.require(actor, "change a seat", Role.CUSTOMER);
        return transactions.inTransaction(() -> {
            Reservation reservation = reservations.findById(reservationId)
                    .orElseThrow(() -> new NotFoundException(TRIP_NOT_FOUND));
            if (reservation.getCustomerId() != actor.getId()) {
                throw new AccessDeniedException("You can only change the seat on your own trips.");
            }
            switch (reservation.statusAt(clock)) {
                case CANCELLED -> throw new BookingException(SEAT_CHANGE_CANCELLED);
                case COMPLETED -> throw new BookingException(SEAT_CHANGE_DEPARTED);
                default -> { }
            }

            Seat newSeat = seats.findById(newSeatId)
                    .orElseThrow(() -> new NotFoundException("This seat was not found."));
            if (newSeat.flightId() != reservation.getFlight().getId()) {
                throw new BookingException(SEAT_NOT_ON_FLIGHT);
            }
            if (newSeat.id() == reservation.getSeat().id()) {
                throw new BookingException(SAME_SEAT);
            }
            boolean taken = seats.findByFlight(newSeat.flightId()).stream()
                    .filter(availability -> availability.seat().id() == newSeat.id())
                    .anyMatch(SeatAvailability::taken);
            if (taken) {
                throw seatTaken(newSeat);
            }

            reservation.changeSeat(newSeat);
            // The unique index still guards a race with another booking for the same seat.
            reservations.updateSeat(reservationId, newSeat);
            return new ReservationDetails(reservation.summaryAt(clock), payments.paymentFor(reservationId).orElse(null));
        });
    }

    private BookingConfirmation bookInTransaction(User actor, BookingRequest request) {
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        Flight flight = flights.findById(request.flightId())
                .orElseThrow(() -> new NotFoundException("This flight was not found."));
        if (flight.getStatus() != FlightStatus.SCHEDULED || flight.isDeparted(clock)) {
            throw new BookingException(FlightSearchController.FLIGHT_UNAVAILABLE);
        }

        // A BookingException, not a ValidationException: the seat is not a field on the payment form,
        // so the view shows the message and returns to the seat map.
        Seat seat = seats.findById(request.seatId())
                .filter(candidate -> candidate.flightId() == flight.getId())
                .orElseThrow(() -> new BookingException(SEAT_NOT_ON_FLIGHT));
        boolean taken = seats.findByFlight(flight.getId()).stream()
                .filter(availability -> availability.seat().id() == seat.id())
                .anyMatch(SeatAvailability::taken);
        if (taken) {
            throw seatTaken(seat);
        }
        if (reservations.hasConfirmedOnFlight(actor.getId(), flight.getId())) {
            throw new BookingException("You already have a seat on this flight.");
        }
        if (flight.getPriceCents() != request.expectedPriceCents()) {
            String currentPrice = BigDecimal.valueOf(flight.getPriceCents(), 2).toPlainString();
            throw new BookingException("The fare changed to $" + currentPrice + ". Review and try again.");
        }

        Payment payment = payments.processPayment(actor, request.payment(), flight.getPriceCents());
        Reservation reservation = Reservation.pending(actor.getId(), seat, flight, flight.getPriceCents());
        reservation.confirm(codes.next(reservations), now);
        Reservation saved = reservations.insert(reservation);
        Payment recorded = payments.recordPayment(saved.getId(), payment);
        return new BookingConfirmation(saved, recorded);
    }

    private static BookingException seatTaken(Seat seat) {
        return new BookingException("Seat " + seat.label() + " was just taken. Pick another seat.");
    }
}