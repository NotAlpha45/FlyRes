package com.flyres.data.sqlite;

import com.flyres.common.exception.DataAccessException;
import com.flyres.data.DatabaseManager;
import com.flyres.domain.booking.Payment;
import com.flyres.domain.booking.PaymentStatus;
import com.flyres.repository.PaymentRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * SQLite implementation of {@link PaymentRepository}. Stores only the cardholder name, the last 4 digits,
 * the amount, the status and the timestamps: never the full card number or the CVV, which no class past
 * the control ever receives.
 */
public final class SqlitePaymentRepository implements PaymentRepository {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final DatabaseManager database;

    public SqlitePaymentRepository(DatabaseManager database) {
        this.database = database;
    }

    @Override
    public Payment insert(long reservationId, Payment payment) {
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalArgumentException("Only a COMPLETED payment can be saved, not " + payment.getStatus());
        }
        String sql = """
                INSERT INTO payments (reservation_id, cardholder_name, card_last4, amount_cents, status, paid_at, refunded_at)
                VALUES (?, ?, ?, ?, ?, ?, NULL)
                RETURNING id""";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, reservationId);
            statement.setString(2, payment.getCardholderName());
            statement.setString(3, payment.getCardLast4());
            statement.setLong(4, payment.getAmountCents());
            statement.setString(5, payment.getStatus().name());
            statement.setString(6, TIMESTAMP.format(payment.getPaidAt().orElseThrow()));
            try (ResultSet row = statement.executeQuery()) {
                row.next();
                return Payment.restore(row.getLong(1), reservationId, payment.getCardholderName(),
                        payment.getCardLast4(), payment.getAmountCents(), payment.getStatus(),
                        payment.getPaidAt().orElseThrow(), null);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the payment for reservation " + reservationId, e);
        }
    }

    @Override
    public Optional<Payment> findByReservationId(long reservationId) {
        String sql = """
                SELECT id, reservation_id, cardholder_name, card_last4, amount_cents, status, paid_at, refunded_at
                FROM payments WHERE reservation_id = ?""";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, reservationId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    return Optional.empty();
                }
                String refundedAt = row.getString("refunded_at");
                return Optional.of(Payment.restore(
                        row.getLong("id"),
                        row.getLong("reservation_id"),
                        row.getString("cardholder_name"),
                        row.getString("card_last4"),
                        row.getLong("amount_cents"),
                        PaymentStatus.valueOf(row.getString("status")),
                        LocalDateTime.parse(row.getString("paid_at")),
                        refundedAt == null ? null : LocalDateTime.parse(refundedAt)));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the payment for reservation " + reservationId, e);
        }
    }
}
