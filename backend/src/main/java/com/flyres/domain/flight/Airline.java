package com.flyres.domain.flight;

import java.util.Objects;

/** An airline, identified by its 2-character IATA code. Seed data only: there is no management screen. */
public record Airline(long id, String code, String name) {

    public Airline {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
    }
}
