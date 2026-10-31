package com.flyres.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SeedDataTest {

    private DatabaseManager database;
    private Connection connection;

    @BeforeEach
    void setUp() {
        database = DatabaseManager.forTestingWithSeedData();
        connection = database.getConnection();
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    @Test
    void seed_everyFlight_departsInTheFuture() throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT flight_number, departure_time FROM flights")) {
            boolean any = false;
            while (result.next()) {
                any = true;
                LocalDateTime departure = LocalDateTime.parse(result.getString("departure_time"));
                assertTrue(departure.isAfter(now), result.getString("flight_number") + " departs at " + departure);
            }
            assertTrue(any, "seed has no flights");
        }
    }

    @Test
    void seed_everyFlight_hasOneSeatPerLayoutPosition() throws SQLException {
        String sql = """
                SELECT f.id, a.seat_rows * a.seats_per_row AS expected, COUNT(s.id) AS actual
                FROM flights f
                JOIN aircraft a ON a.id = f.aircraft_id
                LEFT JOIN seats s ON s.flight_id = f.id
                GROUP BY f.id""";
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                assertEquals(result.getInt("expected"), result.getInt("actual"), "flight " + result.getLong("id"));
            }
        }
    }

    @Test
    void seed_demoAccounts_haveExpectedRoles() throws SQLException {
        Map<String, String> roles = new HashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT email, role FROM users")) {
            while (result.next()) {
                roles.put(result.getString("email"), result.getString("role"));
            }
        }
        assertEquals(Map.of(
                "admin@flyres.ca", "ADMIN",
                "agent@flyres.ca", "AGENT",
                "maya@example.com", "CUSTOMER",
                "omar@example.com", "CUSTOMER"), roles);
    }

    @Test
    void seed_promotionDeliveries_areEmpty() throws SQLException {
        assertEquals(0, TestRows.count(connection, "promotion_deliveries"));
        assertTrue(TestRows.count(connection, "promotions") > 0);
    }
}
