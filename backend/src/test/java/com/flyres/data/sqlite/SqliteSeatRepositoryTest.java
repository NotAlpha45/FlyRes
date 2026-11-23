package com.flyres.data.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flyres.data.DatabaseManager;
import com.flyres.domain.flight.Seat;
import com.flyres.domain.flight.SeatAvailability;
import com.flyres.fixtures.FlightFixture;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqliteSeatRepositoryTest {

    private DatabaseManager database;
    private SqliteSeatRepository seats;

    @BeforeEach
    void setUp() throws SQLException {
        database = DatabaseManager.forTesting();
        FlightFixture.insert(database.getConnection());
        seats = new SqliteSeatRepository(database);
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    private SeatAvailability seat(long flightId, String label) {
        return seats.findByFlight(flightId).stream()
                .filter(s -> s.seat().label().equals(label)).findFirst().orElseThrow();
    }

    @Test
    void findByFlight_returnsEverySeatInMapOrder() {
        // WS aircraft: 3 rows x 2 seats.
        List<String> labels = seats.findByFlight(FlightFixture.WS200).stream().map(s -> s.seat().label()).toList();

        assertEquals(List.of("1A", "1B", "2A", "2B", "3A", "3B"), labels);
    }

    @Test
    void findByFlight_rowsTenAndAbove_sortNumerically() throws SQLException {
        try (PreparedStatement insert = database.getConnection()
                .prepareStatement("INSERT INTO seats (flight_id, seat_label) VALUES (?, ?)")) {
            for (int row = 4; row <= 11; row++) {
                insert.setLong(1, FlightFixture.WS200);
                insert.setString(2, row + "A");
                insert.executeUpdate();
            }
        }

        List<String> labels = seats.findByFlight(FlightFixture.WS200).stream().map(s -> s.seat().label()).toList();

        assertEquals("9A", labels.get(labels.size() - 3));
        assertEquals("10A", labels.get(labels.size() - 2));
        assertEquals("11A", labels.get(labels.size() - 1));
    }

    @Test
    void findByFlight_confirmedIsTakenCancelledIsFree() {
        // Fixture: AC104 1A CONFIRMED, 1B CANCELLED.
        assertTrue(seat(FlightFixture.AC104, "1A").taken());
        assertFalse(seat(FlightFixture.AC104, "1B").taken());
        assertFalse(seat(FlightFixture.AC104, "2A").taken());
    }

    @Test
    void findByFlight_afterCancelling_seatIsFreeAgain() throws SQLException {
        try (Statement statement = database.getConnection().createStatement()) {
            statement.execute("UPDATE reservations SET status = 'CANCELLED', cancelled_at = '2030-03-05T00:00:00' "
                    + "WHERE confirmation_code = 'CONF01'");
        }

        assertFalse(seat(FlightFixture.AC104, "1A").taken());
    }

    @Test
    void findById_returnsSeatOrEmpty() {
        Seat first = seats.findByFlight(FlightFixture.AC104).get(0).seat();

        assertEquals(first, seats.findById(first.id()).orElseThrow());
        assertTrue(seats.findById(99_999).isEmpty());
    }
}
