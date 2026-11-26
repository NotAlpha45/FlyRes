package com.flyres.domain.booking;

/** The customer, flight, seat, displayed fare and payment details submitted for one booking. */
public record BookingRequest(long customerId, long flightId, long seatId, long expectedPriceCents,
                             PaymentDetails payment) {
}