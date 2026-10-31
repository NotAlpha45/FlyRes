package com.flyres.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SchemaTest {

    private DatabaseManager database;
    private Connection connection;
    private long seatId;

    @BeforeEach
    void setUp() throws SQLException {
        database = DatabaseManager.forTesting();
        connection = database.getConnection();
        seatId = TestRows.insertCustomerAndSeat(connection);
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    @Test
    void insertReservation_secondConfirmedForSameSeat_rejectedByDatabase() throws SQLException {
        TestRows.insertReservation(connection, "AAAAAA", seatId, "CONFIRMED");

        SQLException error = assertThrows(SQLException.class,
                () -> TestRows.insertReservation(connection, "BBBBBB", seatId, "CONFIRMED"));

        assertTrue(error.getMessage().contains("UNIQUE"), error.getMessage());
        assertEquals(1, TestRows.count(connection, "reservations"));
    }

    @Test
    void insertReservation_confirmedAfterCancelledForSameSeat_allowed() throws SQLException {
        TestRows.insertReservation(connection, "AAAAAA", seatId, "CANCELLED");
        TestRows.insertReservation(connection, "BBBBBB", seatId, "CONFIRMED");

        assertEquals(2, TestRows.count(connection, "reservations"));
    }

    @Test
    void insertReservation_unknownSeat_rejectedByForeignKey() {
        assertThrows(SQLException.class,
                () -> TestRows.insertReservation(connection, "AAAAAA", 999, "CONFIRMED"));
    }

    @Test
    void insertReservation_pendingStatus_rejectedByCheck() {
        assertThrows(SQLException.class,
                () -> TestRows.insertReservation(connection, "AAAAAA", seatId, "PENDING"));
    }
}
