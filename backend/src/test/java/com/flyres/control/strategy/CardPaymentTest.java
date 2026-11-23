package com.flyres.control.strategy;

import static com.flyres.control.strategy.CardPayment.CARDHOLDER_NAME;
import static com.flyres.control.strategy.CardPayment.CARD_NUMBER;
import static com.flyres.control.strategy.CardPayment.CVV;
import static com.flyres.control.strategy.CardPayment.EXPIRY;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flyres.common.exception.ValidationException;
import com.flyres.domain.booking.PaymentDetails;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CardPaymentTest {

    /** "Now" is 15 December 2030. */
    private static final Clock DEC_2030 = Clock.fixed(Instant.parse("2030-12-15T12:00:00Z"), ZoneOffset.UTC);
    private static final YearMonth VALID_EXPIRY = YearMonth.of(2032, 4);

    private static CardPayment card(String number) {
        return new CardPayment(new PaymentDetails("Maya Patel", number, VALID_EXPIRY, "123"));
    }

    private static CardPayment card(String name, String number, YearMonth expiry, String cvv) {
        return new CardPayment(new PaymentDetails(name, number, expiry, cvv));
    }

    private static void assertValid(CardPayment card) {
        assertDoesNotThrow(() -> card.validate(DEC_2030).throwIfAny());
    }

    private static List<String> invalidFields(CardPayment card) {
        ValidationException e = assertThrows(ValidationException.class, () -> card.validate(DEC_2030).throwIfAny());
        return List.copyOf(e.getFieldErrors().keySet());
    }

    // ---------- card number ----------

    @ParameterizedTest
    @ValueSource(strings = {"4242424242424242", "4242 4242 4242 4242", "4242-4242-4242-4242", "4000 0000 0000 0002",
            "4222222222222", "4000000000000000006"})
    void validate_luhnValidNumbers_13To19Digits_pass(String number) {
        assertValid(card(number));
    }

    @Test
    void validate_failsLuhn_cardNumberError() {
        assertEquals(List.of(CARD_NUMBER), invalidFields(card("4242424242424241")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "424242424242", "42424242424242424242", "4242 4242 abcd 4242"})
    void validate_wrongLengthOrNotDigits_cardNumberError(String number) {
        assertEquals(List.of(CARD_NUMBER), invalidFields(card(number)));
    }

    @Test
    void validate_nullNumber_cardNumberError() {
        assertEquals(List.of(CARD_NUMBER), invalidFields(card(null)));
    }

    // ---------- expiry ----------

    @Test
    void validate_expiryThisMonth_passes() {
        assertValid(card("Maya", "4242424242424242", YearMonth.of(2030, 12), "123"));
    }

    @Test
    void validate_expiryLastMonth_expiryError() {
        assertEquals(List.of(EXPIRY), invalidFields(card("Maya", "4242424242424242", YearMonth.of(2030, 11), "123")));
    }

    @Test
    void validate_acrossYearBoundary_decemberExpiryFailsInJanuary() {
        Clock jan2031 = Clock.fixed(Instant.parse("2031-01-01T00:00:00Z"), ZoneOffset.UTC);
        CardPayment december = card("Maya", "4242424242424242", YearMonth.of(2030, 12), "123");
        CardPayment january = card("Maya", "4242424242424242", YearMonth.of(2031, 1), "123");

        assertTrue(december.validate(jan2031).has(EXPIRY));
        assertFalse(january.validate(jan2031).has(EXPIRY));
    }

    @Test
    void validate_missingExpiry_expiryError() {
        assertEquals(List.of(EXPIRY), invalidFields(card("Maya", "4242424242424242", null, "123")));
    }

    // ---------- CVV and name ----------

    @ParameterizedTest
    @ValueSource(strings = {"", "12", "1234", "12a"})
    void validate_cvvNotThreeDigits_cvvError(String cvv) {
        assertEquals(List.of(CVV), invalidFields(card("Maya", "4242424242424242", VALID_EXPIRY, cvv)));
    }

    @Test
    void validate_blankOrLongName_nameError() {
        assertEquals(List.of(CARDHOLDER_NAME), invalidFields(card(" ", "4242424242424242", VALID_EXPIRY, "123")));
        assertEquals(List.of(CARDHOLDER_NAME),
                invalidFields(card("x".repeat(51), "4242424242424242", VALID_EXPIRY, "123")));
    }

    @Test
    void validate_everythingWrong_reportsEveryFieldInFormOrder() {
        assertEquals(List.of(CARDHOLDER_NAME, CARD_NUMBER, EXPIRY, CVV), invalidFields(card("", "1", null, "")));
    }

    // ---------- authorize, last4, name ----------

    @Test
    void authorize_numberEndingIn0002_isDeclined() {
        assertFalse(card("4000 0000 0000 0002").authorize(18900));
        assertTrue(card("4242 4242 4242 4242").authorize(18900));
    }

    @Test
    void last4AndHolderName_areNormalised() {
        CardPayment card = card("  Maya Patel  ", "4242-4242-4242-1881", VALID_EXPIRY, "123");

        assertEquals("1881", card.last4());
        assertEquals("Maya Patel", card.holderName());
    }

    @Test
    void toString_neverContainsTheFullNumberOrCvv() {
        String text = card("Maya", "4242 4242 4242 4242", VALID_EXPIRY, "987").toString();

        assertFalse(text.contains("4242 4242 4242 4242"));
        assertFalse(text.contains("987"));
    }
}
