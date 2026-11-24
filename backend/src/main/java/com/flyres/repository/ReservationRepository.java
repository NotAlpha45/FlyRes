package com.flyres.repository;

import com.flyres.domain.booking.Reservation;
import java.util.Optional;

/** Storage for customer reservations. */
public interface ReservationRepository {

    /** Saves a confirmed reservation and returns it with its database id. */
    Reservation insert(Reservation reservation);

    /** The reservation with this id. */
    Optional<Reservation> findById(long id);

    /** Whether a confirmation code is already assigned. */
    boolean existsByCode(String confirmationCode);

    /** Whether the customer already has a confirmed reservation on the flight. */
    boolean hasConfirmedOnFlight(long customerId, long flightId);
}