package com.flyres.domain.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class PaymentTest {

    private static final LocalDateTime PAID = LocalDateTime.of(2030, 3, 10, 12, 0);
    private static final LocalDateTime REFUNDED = LocalDateTime.of(2030, 3, 11, 9, 0);

    private static Payment initiated() {
        return Payment.initiate("Maya Patel", "4242", 18900);
    }

    private static Payment completed() {
        Payment payment = initiated();
        payment.authorize();
        payment.complete(PAID);
        return payment;
    }

    // ---------- legal transitions ----------

    @Test
    void initiate_startsInitiatedAndUnsaved() {
        Payment payment = initiated();

        assertEquals(PaymentStatus.INITIATED, payment.getStatus());
        assertEquals(0, payment.getId());
        assertTrue(payment.getPaidAt().isEmpty());
    }

    @Test
    void authorizeThenComplete_recordsPaidAt() {
        Payment payment = completed();

        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        assertEquals(PAID, payment.getPaidAt().orElseThrow());
    }

    @Test
    void fail_fromInitiated_isFailed() {
        Payment payment = initiated();
        payment.fail();

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
    }

    @Test
    void fail_fromAuthorized_isFailed() {
        Payment payment = initiated();
        payment.authorize();
        payment.fail();

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
    }

    @Test
    void refund_fromCompleted_recordsRefundedAt() {
        Payment payment = completed();
        payment.refund(REFUNDED);

        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        assertEquals(REFUNDED, payment.getRefundedAt().orElseThrow());
    }

    // ---------- illegal transitions ----------

    @Test
    void complete_fromInitiated_throws() {
        assertThrows(IllegalStateException.class, () -> initiated().complete(PAID));
    }

    @Test
    void refund_fromInitiatedOrAuthorized_throws() {
        assertThrows(IllegalStateException.class, () -> initiated().refund(REFUNDED));
        Payment authorized = initiated();
        authorized.authorize();
        assertThrows(IllegalStateException.class, () -> authorized.refund(REFUNDED));
    }

    @Test
    void authorize_twice_throws() {
        Payment payment = initiated();
        payment.authorize();

        assertThrows(IllegalStateException.class, payment::authorize);
    }

    @Test
    void anything_fromFailed_throws() {
        Payment payment = initiated();
        payment.fail();

        assertThrows(IllegalStateException.class, payment::authorize);
        assertThrows(IllegalStateException.class, () -> payment.complete(PAID));
        assertThrows(IllegalStateException.class, payment::fail);
        assertThrows(IllegalStateException.class, () -> payment.refund(REFUNDED));
    }

    @Test
    void anything_fromRefunded_throws() {
        Payment payment = completed();
        payment.refund(REFUNDED);

        assertThrows(IllegalStateException.class, payment::authorize);
        assertThrows(IllegalStateException.class, () -> payment.complete(PAID));
        assertThrows(IllegalStateException.class, payment::fail);
        assertThrows(IllegalStateException.class, () -> payment.refund(REFUNDED));
    }

    @Test
    void fail_fromCompleted_throws() {
        assertThrows(IllegalStateException.class, () -> completed().fail());
    }

    // ---------- construction ----------

    @Test
    void initiate_zeroOrNegativeAmount_throws() {
        assertThrows(IllegalArgumentException.class, () -> Payment.initiate("Maya", "4242", 0));
        assertThrows(IllegalArgumentException.class, () -> Payment.initiate("Maya", "4242", -1));
    }

    @Test
    void initiate_last4NotFourDigits_throws() {
        assertThrows(IllegalArgumentException.class, () -> Payment.initiate("Maya", "424", 100));
        assertThrows(IllegalArgumentException.class, () -> Payment.initiate("Maya", "42a2", 100));
    }

    // ---------- card data never leaks ----------

    @Test
    void paymentDetailsToString_masksNumberAndHidesCvv() {
        PaymentDetails details = new PaymentDetails("Maya Patel", "4242 4242 4242 4242", YearMonth.of(2031, 4), "123");

        String text = details.toString();

        assertFalse(text.contains("4242424242424242"));
        assertFalse(text.contains("4242 4242 4242 4242"));
        assertFalse(text.contains("123"));
        assertTrue(text.contains("**** **** **** 4242"));
    }

    @Test
    void paymentDetailsToString_junkNumber_isFullyMasked() {
        assertTrue(new PaymentDetails("Maya", "12", null, null).toString().contains("cardNumber=****,"));
    }

    @Test
    void paymentToString_showsOnlyLast4() {
        assertTrue(completed().toString().contains("****4242"));
    }
}
