package com.flyres.domain.flight;

import java.util.Objects;

/** An airport, identified by its 3-letter IATA code. Seed data only: there is no management screen. */
public record Airport(long id, String code, String name, String city, String country) {

    public Airport {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(country, "country");
    }
}
