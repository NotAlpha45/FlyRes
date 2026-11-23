package com.flyres.repository;

import com.flyres.domain.flight.Seat;
import com.flyres.domain.flight.SeatAvailability;
import java.util.List;
import java.util.Optional;

/** Access to the seats of each flight. */
public interface SeatRepository {

    /** Every seat of the flight with whether it has a CONFIRMED reservation, in seat-map order. */
    List<SeatAvailability> findByFlight(long flightId);

    /** The seat with this id. */
    Optional<Seat> findById(long seatId);
}
