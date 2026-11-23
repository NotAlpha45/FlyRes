package com.flyres.domain.flight;

import java.util.Objects;

/** A seat and whether it currently has a {@code CONFIRMED} reservation. */
public record SeatAvailability(Seat seat, boolean taken) {

    public SeatAvailability {
        Objects.requireNonNull(seat, "seat");
    }
}
