package com.flyres.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Inserts the minimum rows that schema-level tests need, using raw SQL. */
final class TestRows {

    private TestRows() {
    }

    /** Creates a customer, a flight and one seat; returns the seat id. Customer id is 1. */
    static long insertCustomerAndSeat(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO users (id, role, email, password_hash, password_salt, first_name, last_name, created_at)
                    VALUES (1, 'CUSTOMER', 'test@example.com', 'hash', 'salt', 'Test', 'Customer', '2026-01-01T00:00:00')""");
            statement.execute("""
                    INSERT INTO airports (id, iata_code, name, city, country) VALUES
                        (1, 'AAA', 'Origin', 'Origin City', 'Canada'),
                        (2, 'BBB', 'Destination', 'Destination City', 'Canada')""");
            statement.execute("INSERT INTO airlines (id, iata_code, name) VALUES (1, 'TT', 'Test Air')");
            statement.execute("""
                    INSERT INTO aircraft (id, airline_id, registration, model, seat_rows, seats_per_row)
                    VALUES (1, 1, 'C-TEST', 'Test Jet', 2, 2)""");
            statement.execute("INSERT INTO routes (id, origin_airport_id, destination_airport_id) VALUES (1, 1, 2)");
            statement.execute("""
                    INSERT INTO flights (id, flight_number, route_id, aircraft_id, departure_time, arrival_time, price_cents, status)
                    VALUES (1, 'TT100', 1, 1, '2030-01-01T08:00', '2030-01-01T10:00', 10000, 'SCHEDULED')""");
            statement.execute("INSERT INTO seats (id, flight_id, seat_label) VALUES (1, 1, '1A')");
        }
        return 1;
    }

    static void insertReservation(Connection connection, String code, long seatId, String status) throws SQLException {
        String sql = """
                INSERT INTO reservations (confirmation_code, customer_id, seat_id, amount_cents, status, created_at, cancelled_at)
                VALUES (?, 1, ?, 10000, ?, '2026-01-01T00:00:00', ?)""";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            statement.setLong(2, seatId);
            statement.setString(3, status);
            statement.setString(4, status.equals("CANCELLED") ? "2026-01-02T00:00:00" : null);
            statement.executeUpdate();
        }
    }

    static long count(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return result.getLong(1);
        }
    }
}
