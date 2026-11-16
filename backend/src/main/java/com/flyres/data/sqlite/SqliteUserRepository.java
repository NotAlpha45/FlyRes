package com.flyres.data.sqlite;

import com.flyres.common.exception.DataAccessException;
import com.flyres.common.security.PasswordHash;
import com.flyres.data.DatabaseManager;
import com.flyres.domain.user.Role;
import com.flyres.domain.user.User;
import com.flyres.domain.user.UserFactory;
import com.flyres.repository.NewUser;
import com.flyres.repository.UserCredentials;
import com.flyres.repository.UserRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/** SQLite implementation of {@link UserRepository}. The {@code email} column is {@code COLLATE NOCASE}. */
public final class SqliteUserRepository implements UserRepository {

    /** ISO-8601 to the second, matching the seed data. */
    static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final String COLUMNS =
            "id, role, email, password_hash, password_salt, first_name, last_name, phone, created_at";

    private final DatabaseManager database;

    public SqliteUserRepository(DatabaseManager database) {
        this.database = database;
    }

    @Override
    public Optional<User> findById(long id) {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE id = ?";
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? Optional.of(toUser(row)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load user " + id, e);
        }
    }

    @Override
    public Optional<UserCredentials> findCredentialsByEmail(String email) {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE email = ?";
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    return Optional.empty();
                }
                PasswordHash password = new PasswordHash(row.getString("password_hash"), row.getString("password_salt"));
                return Optional.of(new UserCredentials(toUser(row), password));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not look up user by email", e);
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet row = statement.executeQuery()) {
                return row.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check whether an email is registered", e);
        }
    }

    @Override
    public User insert(NewUser user) {
        String sql = """
                INSERT INTO users (role, email, password_hash, password_salt, first_name, last_name, phone, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id""";
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setString(1, user.role().name());
            statement.setString(2, user.email());
            statement.setString(3, user.password().hash());
            statement.setString(4, user.password().salt());
            statement.setString(5, user.firstName());
            statement.setString(6, user.lastName());
            statement.setString(7, user.phone());
            statement.setString(8, TIMESTAMP.format(user.createdAt()));
            try (ResultSet row = statement.executeQuery()) {
                row.next();
                long id = row.getLong(1);
                return UserFactory.create(user.role(), id, user.email(), user.firstName(), user.lastName(),
                        user.phone(), user.createdAt());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not insert user", e);
        }
    }

    private Connection connection() {
        return database.getConnection();
    }

    private static User toUser(ResultSet row) throws SQLException {
        return UserFactory.create(
                Role.valueOf(row.getString("role")),
                row.getLong("id"),
                row.getString("email"),
                row.getString("first_name"),
                row.getString("last_name"),
                row.getString("phone"),
                LocalDateTime.parse(row.getString("created_at")));
    }
}
