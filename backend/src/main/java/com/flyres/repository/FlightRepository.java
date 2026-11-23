package com.flyres.repository;

import com.flyres.domain.flight.Flight;
import com.flyres.domain.flight.FlightSummary;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Access to flights and their seat availability. */
public interface FlightRepository {

    /**
     * {@code SCHEDULED} flights from {@code originCode} to {@code destinationCode} departing at or after
     * {@code from} and, when {@code toExclusive} is not null, before it; limited to one airline when
     * {@code airlineCode} is not null. Sorted by departure time, then flight number.
     */
    List<FlightSummary> search(String originCode, String destinationCode, LocalDateTime from,
                               LocalDateTime toExclusive, String airlineCode);

    /** The flight with this id, whatever its status. */
    Optional<Flight> findById(long id);
}
