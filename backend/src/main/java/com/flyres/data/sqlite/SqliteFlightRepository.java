package com.flyres.data.sqlite;

import com.flyres.common.exception.DataAccessException;
import com.flyres.data.DatabaseManager;
import com.flyres.domain.flight.Aircraft;
import com.flyres.domain.flight.Airline;
import com.flyres.domain.flight.Airport;
import com.flyres.domain.flight.Flight;
import com.flyres.domain.flight.FlightStatus;
import com.flyres.domain.flight.FlightSummary;
import com.flyres.domain.flight.Route;
import com.flyres.repository.FlightRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of {@link FlightRepository}. Flight times are stored as {@code yyyy-MM-ddTHH:mm}
 * text, so range filters compare strings in that same format.
 */
public final class SqliteFlightRepository implements FlightRepository {

    /** Matches the stored {@code departure_time} / {@code arrival_time} format. */
    static final DateTimeFormatter FLIGHT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    /** A flight with its route, both airports, aircraft and airline. */
    private static final String FLIGHT_COLUMNS = """
            SELECT f.id, f.flight_number, f.departure_time, f.arrival_time, f.price_cents, f.status,
                   r.id AS route_id,
                   o.id AS origin_id, o.iata_code AS origin_code, o.name AS origin_name,
                   o.city AS origin_city, o.country AS origin_country,
                   d.id AS destination_id, d.iata_code AS destination_code, d.name AS destination_name,
                   d.city AS destination_city, d.country AS destination_country,
                   ac.id AS aircraft_id, ac.registration, ac.model, ac.seat_rows, ac.seats_per_row,
                   al.id AS airline_id, al.iata_code AS airline_code, al.name AS airline_name
            """;

    /** Seats on the flight minus seats with a CONFIRMED reservation. */
    private static final String AVAILABLE_SEATS_COLUMN = """
                 , (SELECT COUNT(*) FROM seats s
                    WHERE s.flight_id = f.id
                      AND NOT EXISTS (SELECT 1 FROM reservations res
                                      WHERE res.seat_id = s.id AND res.status = 'CONFIRMED')) AS available_seats
            """;

    private static final String FLIGHT_JOINS = """
            FROM flights f
            JOIN routes r ON r.id = f.route_id
            JOIN airports o ON o.id = r.origin_airport_id
            JOIN airports d ON d.id = r.destination_airport_id
            JOIN aircraft ac ON ac.id = f.aircraft_id
            JOIN airlines al ON al.id = ac.airline_id
            """;

    private final DatabaseManager database;

    public SqliteFlightRepository(DatabaseManager database) {
        this.database = database;
    }

    @Override
    public List<FlightSummary> search(String originCode, String destinationCode, LocalDateTime from,
                                      LocalDateTime toExclusive, String airlineCode) {
        String sql = FLIGHT_COLUMNS + AVAILABLE_SEATS_COLUMN + FLIGHT_JOINS + """
                WHERE f.status = 'SCHEDULED'
                  AND o.iata_code = ?
                  AND d.iata_code = ?
                  AND f.departure_time >= ?
                  AND (? IS NULL OR f.departure_time < ?)
                  AND (? IS NULL OR al.iata_code = ?)
                ORDER BY f.departure_time, f.flight_number
                """;
        String to = toExclusive == null ? null : FLIGHT_TIME.format(toExclusive);
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, originCode);
            statement.setString(2, destinationCode);
            statement.setString(3, FLIGHT_TIME.format(from));
            statement.setString(4, to);
            statement.setString(5, to);
            statement.setString(6, airlineCode);
            statement.setString(7, airlineCode);
            try (ResultSet rows = statement.executeQuery()) {
                List<FlightSummary> results = new ArrayList<>();
                while (rows.next()) {
                    results.add(new FlightSummary(toFlight(rows), rows.getInt("available_seats")));
                }
                return results;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not search flights", e);
        }
    }

    @Override
    public Optional<Flight> findById(long id) {
        String sql = FLIGHT_COLUMNS + FLIGHT_JOINS + "WHERE f.id = ?";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? Optional.of(toFlight(row)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load flight " + id, e);
        }
    }

    private static Flight toFlight(ResultSet row) throws SQLException {
        Airport origin = new Airport(row.getLong("origin_id"), row.getString("origin_code"),
                row.getString("origin_name"), row.getString("origin_city"), row.getString("origin_country"));
        Airport destination = new Airport(row.getLong("destination_id"), row.getString("destination_code"),
                row.getString("destination_name"), row.getString("destination_city"),
                row.getString("destination_country"));
        Airline airline = new Airline(row.getLong("airline_id"), row.getString("airline_code"),
                row.getString("airline_name"));
        Aircraft aircraft = new Aircraft(row.getLong("aircraft_id"), airline, row.getString("registration"),
                row.getString("model"), row.getInt("seat_rows"), row.getInt("seats_per_row"));
        return new Flight(
                row.getLong("id"),
                row.getString("flight_number"),
                new Route(row.getLong("route_id"), origin, destination),
                aircraft,
                LocalDateTime.parse(row.getString("departure_time")),
                LocalDateTime.parse(row.getString("arrival_time")),
                row.getLong("price_cents"),
                FlightStatus.valueOf(row.getString("status")));
    }
}
