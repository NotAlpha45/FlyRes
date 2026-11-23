package com.flyres.control;

import com.flyres.common.exception.BookingException;
import com.flyres.control.strategy.CardPayment;
import com.flyres.control.strategy.PaymentMethod;
import com.flyres.domain.booking.Payment;
import com.flyres.domain.booking.PaymentDetails;
import com.flyres.domain.booking.PaymentStatus;
import com.flyres.domain.user.Role;
import com.flyres.domain.user.User;
import com.flyres.repository.PaymentRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * &lt;&lt;control&gt;&gt; Simulated card payments. {@link #processPayment} validates and authorizes a card
 * and returns a COMPLETED payment <b>without saving it</b>; the booking saves it with
 * {@link #recordPayment} inside its own transaction, so a failed booking leaves no payment behind.
 * Validation errors use the field names in {@link CardPayment}.
 */
public final class PaymentController {

    /** Shown when the (simulated) issuer declines the card. */
    public static final String DECLINED = "Your card was declined. No charge was made.";

    private final PaymentRepository payments;
    private final Clock clock;

    public PaymentController(PaymentRepository payments, Clock clock) {
        this.payments = Objects.requireNonNull(payments);
        this.clock = Objects.requireNonNull(clock);
    }

    /** Charges a card for {@code amountCents} and returns the completed, unsaved payment. */
    public Payment processPayment(User actor, PaymentDetails details, long amountCents) {
        Objects.requireNonNull(details, "details");
        return processPayment(actor, new CardPayment(details), amountCents);
    }

    /**
     * Charges {@code method} for {@code amountCents}: validates it (every invalid field at once), then
     * INITIATED → AUTHORIZED → COMPLETED, or → FAILED with a {@link BookingException} when declined.
     */
    public Payment processPayment(User actor, PaymentMethod method, long amountCents) {
        Access.require(actor, "make a payment", Role.CUSTOMER, Role.AGENT);
        Objects.requireNonNull(method, "method");
        if (amountCents <= 0) {
            throw new IllegalArgumentException("A payment must be for a positive amount");
        }

        method.validate(clock).throwIfAny();

        Payment payment = Payment.initiate(method.holderName(), method.last4(), amountCents);
        if (!method.authorize(amountCents)) {
            payment.fail();
            throw new BookingException(DECLINED);
        }
        payment.authorize();
        payment.complete(LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS));
        return payment;
    }

    /** Saves a COMPLETED payment for its reservation. Called by the booking inside its transaction. */
    public Payment recordPayment(long reservationId, Payment payment) {
        Objects.requireNonNull(payment, "payment");
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Only a completed payment can be recorded, not " + payment.getStatus());
        }
        return payments.insert(reservationId, payment);
    }
}
