package com.flyres.domain.booking;

import com.flyres.domain.flight.FlightStatus;
import java.util.Objects;

/**
 * A reservation as a customer sees it in a list: the stored reservation plus its <b>effective</b> status,
 * where a CONFIRMED reservation whose flight has departed shows as COMPLETED.
 */
public record ReservationSummary(Reservation reservation, ReservationStatus status) {

    public ReservationSummary {
        Objects.requireNonNull(reservation, "reservation");
        Objects.requireNonNull(status, "status");
    }

    /** True when the trip was cancelled because the airline removed (archived) the flight. */
    public boolean isFlightRemoved() {
        return status == ReservationStatus.CANCELLED
                && reservation.getFlight().getStatus() == FlightStatus.ARCHIVED;
    }
}
