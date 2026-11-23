package com.flyres.repository;

import com.flyres.domain.booking.Payment;
import java.util.Optional;

/** Storage for payments. Only COMPLETED and REFUNDED payments are stored, one per reservation. */
public interface PaymentRepository {

    /** Saves a COMPLETED payment for {@code reservationId} and returns it with its new id. */
    Payment insert(long reservationId, Payment payment);

    /** The payment of the reservation, if any. */
    Optional<Payment> findByReservationId(long reservationId);
}
