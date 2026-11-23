package com.flyres.domain.flight;

import java.util.Objects;

/** A one-way connection from an origin airport to a different destination airport. */
public record Route(long id, Airport origin, Airport destination) {

    public Route {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(destination, "destination");
        if (origin.code().equals(destination.code())) {
            throw new IllegalArgumentException("A route needs two different airports");
        }
    }
}
