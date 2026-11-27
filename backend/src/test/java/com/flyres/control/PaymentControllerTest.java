package com.flyres.control;

import static com.flyres.control.strategy.CardPayment.CARDHOLDER_NAME;
import static com.flyres.control.strategy.CardPayment.CARD_NUMBER;
import static com.flyres.control.strategy.CardPayment.CVV;
import static com.flyres.control.strategy.CardPayment.EXPIRY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flyres.common.exception.AccessDeniedException;
import com.flyres.common.exception.BookingException;
import com.flyres.common.exception.ValidationException;
import com.flyres.common.validation.ValidationErrors;
import com.flyres.control.strategy.PaymentMethod;
import com.flyres.data.DatabaseManager;
import com.flyres.data.sqlite.SqlitePaymentRepository;
import com.flyres.domain.booking.Payment;
import com.flyres.domain.booking.PaymentDetails;
import com.flyres.domain.booking.PaymentStatus;
import com.flyres.domain.user.Role;
import com.flyres.domain.user.User;
import com.flyres.domain.user.UserFactory;
import com.flyres.fixtures.FlightFixture;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentControllerTest {

    private static final Clock NOW = Clock.fixed(Instant.parse("2030-12-15T12:00:30.500Z"), ZoneOffset.UTC);
    private static final YearMonth EXPIRY_OK = YearMonth.of(2032, 4);
    private static final User CUSTOMER = user(Role.CUSTOMER, 1);
    private static final User AGENT = user(Role.AGENT, 2);
    private static final User ADMIN = user(Role.ADMIN, 3);

    private DatabaseManager database;
    private SqlitePaymentRepository repository;
    private PaymentController payments;

    private static User user(Role role, long id) {
        return UserFactory.create(role, id, role.name().toLowerCase() + "@example.com", "Test", "User", null,
                LocalDateTime.of(2030, 1, 1, 0, 0));
    }

    private static PaymentDetails card(String number) {
        return new PaymentDetails("Maya Patel", number, EXPIRY_OK, "123");
    }

    @BeforeEach
    void setUp() throws SQLException {
        database = DatabaseManager.forTesting();
        FlightFixture.insert(database.getConnection());
        repository = new SqlitePaymentRepository(database);
        payments = new PaymentController(repository, NOW);
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    @Test
    void processPayment_validCard_returnsCompletedPaymentWithoutSavingIt() {
        Payment payment = payments.processPayment(CUSTOMER, card("4242 4242 4242 4242"), 18900);

        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        assertEquals("4242", payment.getCardLast4());
        assertEquals("Maya Patel", payment.getCardholderName());
        assertEquals(18900, payment.getAmountCents());
        assertEquals(LocalDateTime.of(2030, 12, 15, 12, 0, 30), payment.getPaidAt().orElseThrow());
        assertEquals(0, payment.getId());
        assertTrue(repository.findByReservationId(1).isEmpty());
    }

    @Test
    void processPayment_asAgent_succeeds() {
        assertEquals(PaymentStatus.COMPLETED,
                payments.processPayment(AGENT, card("4242424242424242"), 100).getStatus());
    }

    @Test
    void processPayment_declinedCard_throwsBookingExceptionAndSavesNothing() {
        BookingException e = assertThrows(BookingException.class,
                () -> payments.processPayment(CUSTOMER, card("4000 0000 0000 0002"), 18900));

        assertEquals(PaymentController.DECLINED, e.getMessage());
        assertTrue(repository.findByReservationId(1).isEmpty());
    }

    @Test
    void processPayment_invalidCard_throwsValidationExceptionWithAllFields() {
        PaymentDetails details = new PaymentDetails("", "1234", YearMonth.of(2030, 11), "12");

        ValidationException e = assertThrows(ValidationException.class,
                () -> payments.processPayment(CUSTOMER, details, 18900));

        assertEquals(List.of(CARDHOLDER_NAME, CARD_NUMBER, EXPIRY, CVV), List.copyOf(e.getFieldErrors().keySet()));
    }

    @Test
    void processPayment_errorMessages_neverContainCardData() {
        PaymentDetails details = new PaymentDetails("Maya", "4242424242424241", EXPIRY_OK, "987");

        ValidationException e = assertThrows(ValidationException.class,
                () -> payments.processPayment(CUSTOMER, details, 18900));

        e.getFieldErrors().values().forEach(message -> {
            assertTrue(!message.contains("4242424242424241") && !message.contains("987"), message);
        });
    }

    @Test
    void processPayment_asAdmin_throwsAccessDeniedException() {
        assertThrows(AccessDeniedException.class,
                () -> payments.processPayment(ADMIN, card("4242424242424242"), 18900));
    }

    @Test
    void processPayment_nonPositiveAmount_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> payments.processPayment(CUSTOMER, card("4242424242424242"), 0));
    }

    @Test
    void processPayment_anyPaymentMethod_isUsedThroughTheStrategyInterface() {
        PaymentMethod voucher = new PaymentMethod() {
            @Override
            public ValidationErrors validate(Clock clock) {
                return new ValidationErrors();
            }

            @Override
            public boolean authorize(long amountCents) {
                return amountCents <= 20000;
            }

            @Override
            public String last4() {
                return "0000";
            }

            @Override
            public String holderName() {
                return "Gift voucher";
            }
        };

        assertEquals("Gift voucher", payments.processPayment(CUSTOMER, voucher, 18900).getCardholderName());
        assertThrows(BookingException.class, () -> payments.processPayment(CUSTOMER, voucher, 30000));
    }

    @Test
    void recordPayment_completedPayment_isSavedForTheReservation() {
        Payment payment = payments.processPayment(CUSTOMER, card("4242424242424242"), 18900);

        Payment saved = payments.recordPayment(1, payment);

        assertTrue(saved.getId() > 0);
        assertEquals(PaymentStatus.COMPLETED, repository.findByReservationId(1).orElseThrow().getStatus());
    }

    @Test
    void refund_completedPayment_marksRefundedAtNow() {
        payments.recordPayment(1, payments.processPayment(CUSTOMER, card("4242424242424242"), 18900));

        Payment refunded = payments.refund(1);

        assertEquals(PaymentStatus.REFUNDED, refunded.getStatus());
        assertEquals(LocalDateTime.of(2030, 12, 15, 12, 0, 30), refunded.getRefundedAt().orElseThrow());
        assertEquals(PaymentStatus.REFUNDED, repository.findByReservationId(1).orElseThrow().getStatus());
    }

    @Test
    void refund_twice_throwsIllegalState() {
        payments.recordPayment(1, payments.processPayment(CUSTOMER, card("4242424242424242"), 18900));
        payments.refund(1);

        assertThrows(IllegalStateException.class, () -> payments.refund(1));
    }

    @Test
    void refund_noPayment_throwsIllegalState() {
        assertThrows(IllegalStateException.class, () -> payments.refund(1));
    }

    @Test
    void recordPayment_notCompleted_throwsIllegalState() {
        assertThrows(IllegalStateException.class,
                () -> payments.recordPayment(1, Payment.initiate("Maya", "4242", 100)));
    }
}
