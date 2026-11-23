package com.flyres.domain.flight;

import java.util.Objects;

/**
 * A physical aircraft operated by one airline. Its seat layout is {@code seatRows × seatsPerRow};
 * a flight's seats are generated from it when the flight is created.
 */
public record Aircraft(long id, Airline airline, String registration, String model, int seatRows, int seatsPerRow) {

    public Aircraft {
        Objects.requireNonNull(airline, "airline");
        Objects.requireNonNull(registration, "registration");
        Objects.requireNonNull(model, "model");
        if (seatRows < 1 || seatsPerRow < 1) {
            throw new IllegalArgumentException("Seat layout must be at least 1 x 1");
        }
    }

    /** Number of seats on board. */
    public int capacity() {
        return seatRows * seatsPerRow;
    }
}
