package com.flyres.control.strategy;

import com.flyres.common.validation.ValidationErrors;
import java.time.Clock;

/**
 * <b>Strategy</b> for taking a payment. {@code PaymentController} works only against this interface, so a
 * new way to pay (gift card, saved card) is a new implementation, not a change to the control.
 */
public interface PaymentMethod {

    /** Every problem with the details the user typed; empty when the method can be charged. */
    ValidationErrors validate(Clock clock);

    /** Asks the (simulated) issuer to approve {@code amountCents}; false means declined. Call after {@link #validate}. */
    boolean authorize(long amountCents);

    /** The last 4 digits to store, never the full number. */
    String last4();

    /** The name to store with the payment. */
    String holderName();
}
