package com.flyres.data.sqlite;

import com.flyres.common.exception.DataAccessException;
import com.flyres.data.DatabaseManager;
import com.flyres.domain.flight.Seat;
import com.flyres.domain.flight.SeatAvailability;
import com.flyres.repository.SeatRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQLite implementation of {@link SeatRepository}. A seat is taken while it has a CONFIRMED reservation. */
public final class SqliteSeatRepository implements SeatRepository {

    private final DatabaseManager database;

    public SqliteSeatRepository(DatabaseManager database) {
        this.database = database;
    }

    @Override
    public List<SeatAvailability> findByFlight(long flightId) {
        // Labels are "<row><letter>": order by the numeric row, then the letter, so 2A sorts before 10A.
        String sql = """
                SELECT s.id, s.flight_id, s.seat_label,
                       EXISTS (SELECT 1 FROM reservations r
                               WHERE r.seat_id = s.id AND r.status = 'CONFIRMED') AS taken
                FROM seats s
                WHERE s.flight_id = ?
                ORDER BY CAST(substr(s.seat_label, 1, length(s.seat_label) - 1) AS INTEGER),
                         substr(s.seat_label, -1)
                """;
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, flightId);
            try (ResultSet rows = statement.executeQuery()) {
                List<SeatAvailability> seats = new ArrayList<>();
                while (rows.next()) {
                    seats.add(new SeatAvailability(toSeat(rows), rows.getBoolean("taken")));
                }
                return seats;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load seats for flight " + flightId, e);
        }
    }

    @Override
    public Optional<Seat> findById(long seatId) {
        String sql = "SELECT id, flight_id, seat_label FROM seats WHERE id = ?";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, seatId);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? Optional.of(toSeat(row)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load seat " + seatId, e);
        }
    }

    private static Seat toSeat(ResultSet row) throws SQLException {
        return new Seat(row.getLong("id"), row.getLong("flight_id"), row.getString("seat_label"));
    }
}
