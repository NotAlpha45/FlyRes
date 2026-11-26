package com.flyres.domain.booking;

import java.util.Objects;

/** The result of a booking: the confirmed reservation and the payment recorded with it. */
public record BookingConfirmation(Reservation reservation, Payment payment) {

    public BookingConfirmation {
        Objects.requireNonNull(reservation, "reservation");
        Objects.requireNonNull(payment, "payment");
    }
}
