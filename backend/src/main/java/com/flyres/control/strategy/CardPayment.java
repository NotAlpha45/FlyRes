package com.flyres.control.strategy;

import com.flyres.common.validation.ValidationErrors;
import com.flyres.domain.booking.PaymentDetails;
import java.time.Clock;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Simulated credit card payment. Validation: the number has 13–19 digits (spaces and dashes are ignored)
 * and passes the Luhn check; the card has not expired (it is valid through its expiry month); the CVV is
 * exactly 3 digits; the cardholder name is given. Any number ending in {@code 0002} is declined, so the
 * failure path can be demonstrated.
 */
public final class CardPayment implements PaymentMethod {

    public static final String CARDHOLDER_NAME = "cardholderName";
    public static final String CARD_NUMBER = "cardNumber";
    public static final String EXPIRY = "expiry";
    public static final String CVV = "cvv";

    public static final int NAME_MAX_LENGTH = 50;
    private static final String DECLINE_SUFFIX = "0002";

    private final PaymentDetails details;
    private final String digits;

    public CardPayment(PaymentDetails details) {
        this.details = Objects.requireNonNull(details, "details");
        this.digits = details.digits();
    }

    @Override
    public ValidationErrors validate(Clock clock) {
        ValidationErrors errors = new ValidationErrors();

        String name = details.cardholderName();
        if (name == null || name.isBlank()) {
            errors.add(CARDHOLDER_NAME, "Enter the name on the card.");
        } else if (name.strip().length() > NAME_MAX_LENGTH) {
            errors.add(CARDHOLDER_NAME, "Use at most " + NAME_MAX_LENGTH + " characters.");
        }

        if (digits.isEmpty()) {
            errors.add(CARD_NUMBER, "Enter the card number.");
        } else if (!digits.matches("[0-9]{13,19}")) {
            errors.add(CARD_NUMBER, "Enter a card number of 13 to 19 digits.");
        } else if (!passesLuhn(digits)) {
            errors.add(CARD_NUMBER, "This card number is not valid. Check it and try again.");
        }

        YearMonth expiry = details.expiry();
        if (expiry == null) {
            errors.add(EXPIRY, "Choose the expiry month and year.");
        } else if (expiry.isBefore(YearMonth.now(clock))) {
            errors.add(EXPIRY, "This card has expired.");
        }

        if (details.cvv() == null || !details.cvv().matches("[0-9]{3}")) {
            errors.add(CVV, "Enter the 3-digit security code.");
        }
        return errors;
    }

    @Override
    public boolean authorize(long amountCents) {
        return !digits.endsWith(DECLINE_SUFFIX);
    }

    @Override
    public String last4() {
        return digits.substring(digits.length() - 4);
    }

    @Override
    public String holderName() {
        return details.cardholderName().strip();
    }

    /** The Luhn (mod 10) checksum used by every card scheme. */
    static boolean passesLuhn(String number) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = number.charAt(i) - '0';
            if (doubleIt) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }

    @Override
    public String toString() {
        return "CardPayment[" + details + "]";
    }
}
