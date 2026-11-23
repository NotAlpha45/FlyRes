package com.flyres.domain.booking;

import java.time.YearMonth;

/**
 * Card details typed by the user, as given. The control validates them; nothing here is ever stored or
 * logged except the last 4 digits. {@link #toString()} masks the number and hides the CVV, so the record
 * cannot leak card data into a log or an exception message.
 */
public record PaymentDetails(String cardholderName, String cardNumber, YearMonth expiry, String cvv) {

    /** The digits of the card number, without spaces or dashes; empty if there is no number. */
    public String digits() {
        return cardNumber == null ? "" : cardNumber.replaceAll("[\\s-]", "");
    }

    @Override
    public String toString() {
        String digits = digits();
        String masked = digits.length() >= 4 && digits.chars().allMatch(Character::isDigit)
                ? "**** **** **** " + digits.substring(digits.length() - 4)
                : "****";
        return "PaymentDetails[cardholderName=" + cardholderName + ", cardNumber=" + masked
                + ", expiry=" + expiry + ", cvv=***]";
    }
}
