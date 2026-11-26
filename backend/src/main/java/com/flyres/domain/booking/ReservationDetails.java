package com.flyres.domain.booking;

import java.util.Objects;
import java.util.Optional;

/** One reservation with its payment record, for the trip details screen. */
public record ReservationDetails(ReservationSummary summary, Payment payment) {

    public ReservationDetails {
        Objects.requireNonNull(summary, "summary");
    }

    /** The payment taken for the reservation; every booking made in the app has one. */
    public Optional<Payment> paymentRecord() {
        return Optional.ofNullable(payment);
    }
}
