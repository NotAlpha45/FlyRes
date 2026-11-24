package com.flyres.control;

import com.flyres.common.exception.BookingException;
import com.flyres.common.exception.NotFoundException;
import com.flyres.common.exception.ValidationException;
import com.flyres.domain.booking.BookingRequest;
import com.flyres.domain.booking.Payment;
import com.flyres.domain.booking.Reservation;
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

/** Coordinates fare validation, simulated payment and reservation creation in one transaction. */
public final class BookingController {

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

    /** Books the chosen seat and records its completed payment atomically. */
    public Reservation book(User actor, BookingRequest request) {
        Access.require(actor, "book a flight", Role.CUSTOMER);
        Objects.requireNonNull(request, "request");
        if (request.customerId() != actor.getId()) {
            throw new com.flyres.common.exception.AccessDeniedException(
                    "You don't have permission to book for another customer.");
        }

        return transactions.inTransaction(() -> bookInTransaction(actor, request));
    }

    private Reservation bookInTransaction(User actor, BookingRequest request) {
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        Flight flight = flights.findById(request.flightId())
                .orElseThrow(() -> new NotFoundException("This flight was not found."));
        if (flight.getStatus() != FlightStatus.SCHEDULED || !flight.getDepartureTime().isAfter(now)) {
            throw new BookingException(FlightSearchController.FLIGHT_UNAVAILABLE);
        }

        Seat seat = seats.findById(request.seatId())
                .filter(candidate -> candidate.flightId() == flight.getId())
                .orElseThrow(() -> new ValidationException("seat", "Choose a seat on this flight."));
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
        payments.recordPayment(saved.getId(), payment);
        return saved;
    }

    private static BookingException seatTaken(Seat seat) {
        return new BookingException("Seat " + seat.label() + " was just taken. Pick another seat.");
    }
}