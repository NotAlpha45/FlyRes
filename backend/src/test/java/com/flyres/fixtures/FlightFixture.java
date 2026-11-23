package com.flyres.fixtures;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * A small, fixed flight network for search tests, independent of the relative dates in seed.sql.
 * "Now" is {@link #NOW} (2030-03-10 12:00); every time below is relative to it.
 *
 * <pre>
 * id  number  route       departure          status     airline  notes
 * 1   AC100   YYC -> YVR  2030-03-10 08:00   SCHEDULED  AC       departed this morning
 * 2   AC102   YYC -> YVR  2030-03-10 12:00   SCHEDULED  AC       departs exactly now: departed
 * 3   AC104   YYC -> YVR  2030-03-10 18:00   SCHEDULED  AC       later today; 1A CONFIRMED, 1B CANCELLED
 * 4   WS200   YYC -> YVR  2030-03-11 09:00   SCHEDULED  WS
 * 5   AC106   YYC -> YVR  2030-03-12 07:00   ARCHIVED   AC       never shown
 * 6   WS300   YYC -> YYZ  2030-03-11 07:00   SCHEDULED  WS       other route
 * 7   AC108   YYC -> YVR  2030-03-11 06:00   SCHEDULED  AC       before WS200 on the same day
 * </pre>
 * AC aircraft: 2 x 2 = 4 seats. WS aircraft: 3 x 2 = 6 seats.
 */
public final class FlightFixture {

    public static final Clock NOW = Clock.fixed(Instant.parse("2030-03-10T12:00:00Z"), ZoneOffset.UTC);

    public static final long AC104 = 3;
    public static final long WS200 = 4;
    public static final long AC106_ARCHIVED = 5;
    public static final long WS300 = 6;
    public static final long AC108 = 7;

    private FlightFixture() {
    }

    /** Inserts the network into an empty database from {@code DatabaseManager.forTesting()}. */
    public static void insert(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO users (id, role, email, password_hash, password_salt, first_name, last_name, created_at)
                    VALUES (1, 'CUSTOMER', 'test@example.com', 'hash', 'salt', 'Test', 'Customer', '2030-01-01T00:00:00')""");
            statement.execute("""
                    INSERT INTO airports (id, iata_code, name, city, country) VALUES
                        (1, 'YYC', 'Calgary International Airport', 'Calgary', 'Canada'),
                        (2, 'YVR', 'Vancouver International Airport', 'Vancouver', 'Canada'),
                        (3, 'YYZ', 'Toronto Pearson International Airport', 'Toronto', 'Canada')""");
            statement.execute("INSERT INTO airlines (id, iata_code, name) VALUES (1, 'AC', 'Air Canada'), (2, 'WS', 'WestJet')");
            statement.execute("""
                    INSERT INTO aircraft (id, airline_id, registration, model, seat_rows, seats_per_row) VALUES
                        (1, 1, 'C-TEST', 'Test Jet', 2, 2),
                        (2, 2, 'C-WEST', 'West Jet', 3, 2)""");
            statement.execute("""
                    INSERT INTO routes (id, origin_airport_id, destination_airport_id) VALUES (1, 1, 2), (2, 1, 3)""");
            statement.execute("""
                    INSERT INTO flights (id, flight_number, route_id, aircraft_id, departure_time, arrival_time, price_cents, status) VALUES
                        (1, 'AC100', 1, 1, '2030-03-10T08:00', '2030-03-10T09:30', 18900, 'SCHEDULED'),
                        (2, 'AC102', 1, 1, '2030-03-10T12:00', '2030-03-10T13:30', 18900, 'SCHEDULED'),
                        (3, 'AC104', 1, 1, '2030-03-10T18:00', '2030-03-10T19:30', 18900, 'SCHEDULED'),
                        (4, 'WS200', 1, 2, '2030-03-11T09:00', '2030-03-11T10:30', 15900, 'SCHEDULED'),
                        (5, 'AC106', 1, 1, '2030-03-12T07:00', '2030-03-12T08:30', 18900, 'ARCHIVED'),
                        (6, 'WS300', 2, 2, '2030-03-11T07:00', '2030-03-11T13:00', 32900, 'SCHEDULED'),
                        (7, 'AC108', 1, 1, '2030-03-11T06:00', '2030-03-11T07:30', 17900, 'SCHEDULED')""");
            statement.execute("""
                    WITH RECURSIVE
                        seat_row (n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seat_row WHERE n < 60),
                        seat_col (n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seat_col WHERE n < 10)
                    INSERT INTO seats (flight_id, seat_label)
                    SELECT f.id, seat_row.n || substr('ABCDEFGHIJ', seat_col.n, 1)
                    FROM flights f
                    JOIN aircraft a ON a.id = f.aircraft_id
                    JOIN seat_row ON seat_row.n <= a.seat_rows
                    JOIN seat_col ON seat_col.n <= a.seats_per_row""");
        }
        insertReservation(connection, "CONF01", AC104, "1A", "CONFIRMED");
        insertReservation(connection, "CANC01", AC104, "1B", "CANCELLED");
    }

    /** Adds a reservation for customer 1 on {@code seatLabel} of {@code flightId}. */
    public static void insertReservation(Connection connection, String code, long flightId, String seatLabel,
                                         String status) throws SQLException {
        String sql = """
                INSERT INTO reservations (confirmation_code, customer_id, seat_id, amount_cents, status, created_at, cancelled_at)
                VALUES (?, 1, (SELECT id FROM seats WHERE flight_id = ? AND seat_label = ?), 18900, ?, '2030-03-01T00:00:00', ?)""";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            statement.setLong(2, flightId);
            statement.setString(3, seatLabel);
            statement.setString(4, status);
            statement.setString(5, status.equals("CANCELLED") ? "2030-03-02T00:00:00" : null);
            statement.executeUpdate();
        }
    }
}
