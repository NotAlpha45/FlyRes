package com.flyres.data.sqlite;

import com.flyres.common.exception.BookingException;
import com.flyres.common.exception.DataAccessException;
import com.flyres.data.DatabaseManager;
import com.flyres.domain.booking.Reservation;
import com.flyres.domain.booking.ReservationStatus;
import com.flyres.domain.flight.Flight;
import com.flyres.domain.flight.Seat;
import com.flyres.repository.FlightRepository;
import com.flyres.repository.ReservationRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

/** SQLite implementation of {@link ReservationRepository}. */
public final class SqliteReservationRepository implements ReservationRepository {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /** A reservation with its seat; the flight is loaded through {@link FlightRepository}. */
    private static final String RESERVATION_SELECT = """
            SELECT r.id, r.confirmation_code, r.customer_id, r.amount_cents, r.status, r.created_at,
                   r.cancelled_at, s.id AS seat_id, s.flight_id, s.seat_label
            FROM reservations r JOIN seats s ON s.id = r.seat_id
            """;

    private final DatabaseManager database;
    private final FlightRepository flights;

    public SqliteReservationRepository(DatabaseManager database, FlightRepository flights) {
        this.database = database;
        this.flights = flights;
    }

    @Override
    public Reservation insert(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("Only a confirmed reservation can be saved");
        }
        String sql = """
                INSERT INTO reservations (confirmation_code, customer_id, seat_id, amount_cents, status,
                                          created_at, cancelled_at)
                VALUES (?, ?, ?, ?, 'CONFIRMED', ?, NULL)
                RETURNING id""";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, reservation.getConfirmationCode());
            statement.setLong(2, reservation.getCustomerId());
            statement.setLong(3, reservation.getSeat().id());
            statement.setLong(4, reservation.getAmountCents());
            statement.setString(5, TIMESTAMP.format(reservation.getCreatedAt().orElseThrow()));
            try (ResultSet row = statement.executeQuery()) {
                row.next();
                return Reservation.restore(row.getLong(1), reservation.getConfirmationCode(),
                        reservation.getCustomerId(), reservation.getSeat(), reservation.getFlight(),
                        reservation.getAmountCents(), ReservationStatus.CONFIRMED,
                        reservation.getCreatedAt().orElseThrow(), null);
            }
        } catch (SQLException e) {
            if (isConfirmedSeatCollision(e)) {
                throw new BookingException("Seat " + reservation.getSeat().label()
                        + " was just taken. Pick another seat.");
            }
            throw new DataAccessException("Could not save the reservation", e);
        }
    }

    @Override
    public Optional<Reservation> findById(long id) {
        String sql = RESERVATION_SELECT + "WHERE r.id = ?";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? Optional.of(toReservation(row)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load reservation " + id, e);
        }
    }

    @Override
    public List<Reservation> findByCustomer(long customerId) {
        String sql = RESERVATION_SELECT + "WHERE r.customer_id = ? ORDER BY r.id";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet rows = statement.executeQuery()) {
                List<Reservation> found = new ArrayList<>();
                while (rows.next()) {
                    found.add(toReservation(rows));
                }
                return found;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the customer's reservations", e);
        }
    }

    @Override
    public void markCancelled(long reservationId, LocalDateTime at) {
        String sql = """
                UPDATE reservations SET status = 'CANCELLED', cancelled_at = ?
                WHERE id = ? AND status = 'CONFIRMED'""";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, TIMESTAMP.format(at));
            statement.setLong(2, reservationId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("Reservation " + reservationId + " is not CONFIRMED");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not cancel reservation " + reservationId, e);
        }
    }

    @Override
    public void updateSeat(long reservationId, Seat newSeat) {
        String sql = "UPDATE reservations SET seat_id = ? WHERE id = ? AND status = 'CONFIRMED'";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, newSeat.id());
            statement.setLong(2, reservationId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("Reservation " + reservationId + " is not CONFIRMED");
            }
        } catch (SQLException e) {
            if (isConfirmedSeatCollision(e)) {
                throw new BookingException("Seat " + newSeat.label() + " was just taken. Pick another seat.");
            }
            throw new DataAccessException("Could not change the seat of reservation " + reservationId, e);
        }
    }

    /** Maps one row of {@link #RESERVATION_SELECT}; the flight is loaded with its route, aircraft and status. */
    private Reservation toReservation(ResultSet row) throws SQLException {
        Seat seat = new Seat(row.getLong("seat_id"), row.getLong("flight_id"), row.getString("seat_label"));
        Flight flight = flights.findById(seat.flightId())
                .orElseThrow(() -> new DataAccessException("Reservation flight was not found",
                        new IllegalStateException("The reservation references a missing flight")));
        String cancelledAt = row.getString("cancelled_at");
        return Reservation.restore(row.getLong("id"), row.getString("confirmation_code"),
                row.getLong("customer_id"), seat, flight, row.getLong("amount_cents"),
                ReservationStatus.valueOf(row.getString("status")),
                LocalDateTime.parse(row.getString("created_at")),
                cancelledAt == null ? null : LocalDateTime.parse(cancelledAt));
    }

    @Override
    public boolean existsByCode(String confirmationCode) {
        String sql = "SELECT 1 FROM reservations WHERE confirmation_code = ?";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, confirmationCode);
            try (ResultSet row = statement.executeQuery()) {
                return row.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check the confirmation code", e);
        }
    }

    @Override
    public boolean hasConfirmedOnFlight(long customerId, long flightId) {
        String sql = """
                SELECT 1 FROM reservations r JOIN seats s ON s.id = r.seat_id
                WHERE r.customer_id = ? AND s.flight_id = ? AND r.status = 'CONFIRMED'""";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setLong(2, flightId);
            try (ResultSet row = statement.executeQuery()) {
                return row.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check the customer's reservations", e);
        }
    }

    /**
     * True when the insert broke {@code ux_reservations_confirmed_seat}. Decided by the result code, not
     * the message text: reservations have only two UNIQUE constraints, and the other one
     * ({@code confirmation_code}) is named in the message and pre-checked by the code generator.
     */
    private static boolean isConfirmedSeatCollision(SQLException exception) {
        boolean uniqueViolation = exception instanceof SQLiteException sqlite
                && sqlite.getResultCode() == SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE;
        String message = exception.getMessage();
        return uniqueViolation && (message == null || !message.contains("confirmation_code"));
    }
}