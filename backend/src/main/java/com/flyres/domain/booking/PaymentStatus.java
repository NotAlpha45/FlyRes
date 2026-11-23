package com.flyres.domain.booking;

/**
 * Status of a simulated card payment: {@code INITIATED → AUTHORIZED → COMPLETED}, or {@code → FAILED};
 * and {@code COMPLETED → REFUNDED}. Only COMPLETED and REFUNDED payments are ever stored.
 */
public enum PaymentStatus {
    INITIATED,
    AUTHORIZED,
    COMPLETED,
    FAILED,
    REFUNDED
}
