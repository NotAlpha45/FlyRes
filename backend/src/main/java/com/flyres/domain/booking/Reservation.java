package com.flyres.domain.booking;

import com.flyres.domain.flight.Flight;
import com.flyres.domain.flight.Seat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/** A reservation with guarded state transitions. COMPLETED is derived from the flight departure time. */
public final class Reservation {

    private final long id;
    private String confirmationCode;
    private final long customerId;
    private final Seat seat;
    private final Flight flight;
    private final long amountCents;
    private ReservationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime cancelledAt;

    private Reservation(long id, String confirmationCode, long customerId, Seat seat, Flight flight,
                        long amountCents, ReservationStatus status, LocalDateTime createdAt,
                        LocalDateTime cancelledAt) {
        this.id = id;
        this.confirmationCode = confirmationCode;
        this.customerId = customerId;
        this.seat = Objects.requireNonNull(seat, "seat");
        this.flight = Objects.requireNonNull(flight, "flight");
        this.amountCents = amountCents;
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = createdAt;
        this.cancelledAt = cancelledAt;
        if (amountCents < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        if (seat.flightId() != flight.getId()) {
            throw new IllegalArgumentException("Seat must belong to the reservation flight");
        }
    }

    /** Creates a PENDING reservation, which remains in memory until confirmed. */
    public static Reservation pending(long customerId, Seat seat, Flight flight, long amountCents) {
        return new Reservation(0, null, customerId, seat, flight, amountCents,
                ReservationStatus.PENDING, null, null);
    }

    /** Rebuilds a persisted reservation. */
    public static Reservation restore(long id, String confirmationCode, long customerId, Seat seat, Flight flight,
                                      long amountCents, ReservationStatus status, LocalDateTime createdAt,
                                      LocalDateTime cancelledAt) {
        if (status != ReservationStatus.CONFIRMED && status != ReservationStatus.CANCELLED) {
            throw new IllegalArgumentException("Only stored reservation states can be restored");
        }
        return new Reservation(id, confirmationCode, customerId, seat, flight, amountCents,
                status, Objects.requireNonNull(createdAt, "createdAt"), cancelledAt);
    }

    /** PENDING → CONFIRMED at {@code at}. */
    public void confirm(String code, LocalDateTime at) {
        require(ReservationStatus.PENDING, "confirm");
        if (code == null || code.length() != 6) {
            throw new IllegalArgumentException("Confirmation code must contain 6 characters");
        }
        confirmationCode = code;
        createdAt = Objects.requireNonNull(at, "at");
        status = ReservationStatus.CONFIRMED;
    }

    /** CONFIRMED → CANCELLED. */
    public void cancel(LocalDateTime at) {
        require(ReservationStatus.CONFIRMED, "cancel");
        cancelledAt = Objects.requireNonNull(at, "at");
        status = ReservationStatus.CANCELLED;
    }

    /** Returns COMPLETED after departure without changing the stored CONFIRMED state. */
    public ReservationStatus statusAt(Clock clock) {
        if (status == ReservationStatus.CONFIRMED
                && !flight.getDepartureTime().isAfter(LocalDateTime.now(clock))) {
            return ReservationStatus.COMPLETED;
        }
        return status;
    }

    private void require(ReservationStatus expected, String action) {
        if (status != expected) {
            throw new IllegalStateException("Cannot " + action + " a reservation that is " + status);
        }
    }

    public long getId() {
        return id;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }

    public long getCustomerId() {
        return customerId;
    }

    public Seat getSeat() {
        return seat;
    }

    public Flight getFlight() {
        return flight;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Optional<LocalDateTime> getCreatedAt() {
        return Optional.ofNullable(createdAt);
    }

    public Optional<LocalDateTime> getCancelledAt() {
        return Optional.ofNullable(cancelledAt);
    }
}