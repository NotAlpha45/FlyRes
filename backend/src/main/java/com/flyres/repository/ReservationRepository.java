package com.flyres.repository;

import com.flyres.domain.booking.Reservation;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Storage for customer reservations. */
public interface ReservationRepository {

    /** Saves a confirmed reservation and returns it with its database id. */
    Reservation insert(Reservation reservation);

    /** The reservation with this id. */
    Optional<Reservation> findById(long id);

    /** Every reservation the customer has made, confirmed or cancelled, with its flight and seat. */
    List<Reservation> findByCustomer(long customerId);

    /**
     * Marks a CONFIRMED reservation CANCELLED at {@code at}, which frees its seat. Throws
     * {@link IllegalStateException} if the reservation is not CONFIRMED.
     */
    void markCancelled(long reservationId, LocalDateTime at);

    /** Whether a confirmation code is already assigned. */
    boolean existsByCode(String confirmationCode);

    /** Whether the customer already has a confirmed reservation on the flight. */
    boolean hasConfirmedOnFlight(long customerId, long flightId);
}