package com.flyres.domain.flight;

import java.util.Objects;

/** One physical seat on one flight, labelled row number plus letter ({@code 1A}, {@code 12C}). */
public record Seat(long id, long flightId, String label) {

    public Seat {
        Objects.requireNonNull(label, "label");
    }
}
