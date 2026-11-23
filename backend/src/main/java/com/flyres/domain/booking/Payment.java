package com.flyres.domain.booking;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A simulated card payment (<b>State pattern</b>: each transition is a method that checks the current
 * status). It keeps only the cardholder name and the last 4 digits of the card: the full number and the
 * CVV never reach this class.
 * <pre>
 * INITIATED --authorize()--> AUTHORIZED --complete()--> COMPLETED --refund()--> REFUNDED
 *     |                          |
 *     +--------fail()------------+--> FAILED
 * </pre>
 * Every other transition throws {@link IllegalStateException}.
 */
public final class Payment {

    private static final Pattern LAST4 = Pattern.compile("^[0-9]{4}$");

    private final long id;
    private final long reservationId;
    private final String cardholderName;
    private final String cardLast4;
    private final long amountCents;
    private PaymentStatus status;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;

    private Payment(long id, long reservationId, String cardholderName, String cardLast4, long amountCents,
                    PaymentStatus status, LocalDateTime paidAt, LocalDateTime refundedAt) {
        this.id = id;
        this.reservationId = reservationId;
        this.cardholderName = Objects.requireNonNull(cardholderName, "cardholderName");
        this.cardLast4 = Objects.requireNonNull(cardLast4, "cardLast4");
        this.amountCents = amountCents;
        this.status = Objects.requireNonNull(status, "status");
        this.paidAt = paidAt;
        this.refundedAt = refundedAt;
        if (!LAST4.matcher(cardLast4).matches()) {
            throw new IllegalArgumentException("cardLast4 must be exactly 4 digits");
        }
        if (amountCents < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }

    /** A new, unsaved payment for {@code amountCents}, in status INITIATED. */
    public static Payment initiate(String cardholderName, String cardLast4, long amountCents) {
        if (amountCents <= 0) {
            throw new IllegalArgumentException("A payment must be for a positive amount");
        }
        return new Payment(0, 0, cardholderName, cardLast4, amountCents, PaymentStatus.INITIATED, null, null);
    }

    /** Rebuilds a stored payment. Used by the data layer. */
    public static Payment restore(long id, long reservationId, String cardholderName, String cardLast4,
                                  long amountCents, PaymentStatus status, LocalDateTime paidAt,
                                  LocalDateTime refundedAt) {
        return new Payment(id, reservationId, cardholderName, cardLast4, amountCents, status, paidAt, refundedAt);
    }

    /** INITIATED → AUTHORIZED: the card accepted the charge. */
    public void authorize() {
        require(PaymentStatus.INITIATED, "authorize");
        status = PaymentStatus.AUTHORIZED;
    }

    /** AUTHORIZED → COMPLETED: the money was taken at {@code at}. */
    public void complete(LocalDateTime at) {
        require(PaymentStatus.AUTHORIZED, "complete");
        paidAt = Objects.requireNonNull(at, "at");
        status = PaymentStatus.COMPLETED;
    }

    /** INITIATED or AUTHORIZED → FAILED: the card was declined. */
    public void fail() {
        if (status != PaymentStatus.INITIATED && status != PaymentStatus.AUTHORIZED) {
            throw illegal("fail");
        }
        status = PaymentStatus.FAILED;
    }

    /** COMPLETED → REFUNDED: the full amount was returned at {@code at}. */
    public void refund(LocalDateTime at) {
        require(PaymentStatus.COMPLETED, "refund");
        refundedAt = Objects.requireNonNull(at, "at");
        status = PaymentStatus.REFUNDED;
    }

    private void require(PaymentStatus expected, String action) {
        if (status != expected) {
            throw illegal(action);
        }
    }

    private IllegalStateException illegal(String action) {
        return new IllegalStateException("Cannot " + action + " a payment that is " + status);
    }

    /** 0 until the payment is saved. */
    public long getId() {
        return id;
    }

    /** 0 until the payment is saved with its reservation. */
    public long getReservationId() {
        return reservationId;
    }

    public String getCardholderName() {
        return cardholderName;
    }

    public String getCardLast4() {
        return cardLast4;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Optional<LocalDateTime> getPaidAt() {
        return Optional.ofNullable(paidAt);
    }

    public Optional<LocalDateTime> getRefundedAt() {
        return Optional.ofNullable(refundedAt);
    }

    @Override
    public String toString() {
        return "Payment[id=" + id + ", card=****" + cardLast4 + ", amountCents=" + amountCents + ", " + status + "]";
    }
}
