package com.flyres.data.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flyres.common.exception.DataAccessException;
import com.flyres.data.DatabaseManager;
import com.flyres.domain.booking.Payment;
import com.flyres.domain.booking.PaymentStatus;
import com.flyres.fixtures.FlightFixture;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqlitePaymentRepositoryTest {

    /** FlightFixture's CONF01 reservation. */
    private static final long RESERVATION_ID = 1;
    private static final LocalDateTime PAID = LocalDateTime.of(2030, 3, 10, 12, 0, 5);

    private DatabaseManager database;
    private SqlitePaymentRepository payments;

    @BeforeEach
    void setUp() throws SQLException {
        database = DatabaseManager.forTesting();
        FlightFixture.insert(database.getConnection());
        payments = new SqlitePaymentRepository(database);
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    private static Payment completed() {
        Payment payment = Payment.initiate("Maya Patel", "4242", 18900);
        payment.authorize();
        payment.complete(PAID);
        return payment;
    }

    @Test
    void insert_thenFind_roundTripsEveryField() {
        Payment saved = payments.insert(RESERVATION_ID, completed());
        Payment found = payments.findByReservationId(RESERVATION_ID).orElseThrow();

        assertTrue(saved.getId() > 0);
        assertEquals(saved.getId(), found.getId());
        assertEquals(RESERVATION_ID, found.getReservationId());
        assertEquals("Maya Patel", found.getCardholderName());
        assertEquals("4242", found.getCardLast4());
        assertEquals(18900, found.getAmountCents());
        assertEquals(PaymentStatus.COMPLETED, found.getStatus());
        assertEquals(PAID, found.getPaidAt().orElseThrow());
        assertTrue(found.getRefundedAt().isEmpty());
    }

    @Test
    void insert_storesOnlyLast4_noColumnCanHoldTheFullNumberOrCvv() throws SQLException {
        payments.insert(RESERVATION_ID, completed());

        List<String> columns = new ArrayList<>();
        try (Statement statement = database.getConnection().createStatement();
             ResultSet row = statement.executeQuery("SELECT * FROM payments")) {
            ResultSetMetaData meta = row.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                columns.add(meta.getColumnName(i));
            }
            row.next();
            assertEquals("4242", row.getString("card_last4"));
        }
        assertEquals(List.of("id", "reservation_id", "cardholder_name", "card_last4", "amount_cents", "status",
                "paid_at", "refunded_at"), columns);
    }

    @Test
    void insert_notCompleted_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> payments.insert(RESERVATION_ID, Payment.initiate("Maya", "4242", 100)));
    }

    @Test
    void insert_secondPaymentForSameReservation_isRejectedByTheDatabase() {
        payments.insert(RESERVATION_ID, completed());

        assertThrows(DataAccessException.class, () -> payments.insert(RESERVATION_ID, completed()));
    }

    @Test
    void findByReservationId_none_isEmpty() {
        assertTrue(payments.findByReservationId(RESERVATION_ID).isEmpty());
    }
}
