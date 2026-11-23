package com.flyres.domain.flight;

import java.util.Objects;

/** A flight in a result list, with the number of seats that have no confirmed reservation. */
public record FlightSummary(Flight flight, int availableSeats) {

    public FlightSummary {
        Objects.requireNonNull(flight, "flight");
        if (availableSeats < 0) {
            throw new IllegalArgumentException("Available seats cannot be negative");
        }
    }
}
