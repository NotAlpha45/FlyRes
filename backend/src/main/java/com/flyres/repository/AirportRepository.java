package com.flyres.repository;

import com.flyres.domain.flight.Airport;
import java.util.List;
import java.util.Optional;

/** Read access to airports, which come from seed data only. */
public interface AirportRepository {

    /** Every airport, sorted by city. */
    List<Airport> findAll();

    /** The airport with this IATA code (exact, upper-case). */
    Optional<Airport> findByCode(String code);
}
