package com.flyres.domain.booking;

/** Reservation states; COMPLETED is derived from departure and is never stored. */
public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED
}