package com.flyres.repository;

import com.flyres.domain.flight.Airline;
import java.util.List;
import java.util.Optional;

/** Read access to airlines, which come from seed data only. */
public interface AirlineRepository {

    /** Every airline, sorted by name. */
    List<Airline> findAll();

    /** The airline with this IATA code (exact, upper-case). */
    Optional<Airline> findByCode(String code);
}
