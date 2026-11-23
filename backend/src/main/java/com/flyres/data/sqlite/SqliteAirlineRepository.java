package com.flyres.data.sqlite;

import com.flyres.common.exception.DataAccessException;
import com.flyres.data.DatabaseManager;
import com.flyres.domain.flight.Airline;
import com.flyres.repository.AirlineRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQLite implementation of {@link AirlineRepository}. */
public final class SqliteAirlineRepository implements AirlineRepository {

    private final DatabaseManager database;

    public SqliteAirlineRepository(DatabaseManager database) {
        this.database = database;
    }

    @Override
    public List<Airline> findAll() {
        String sql = "SELECT id, iata_code, name FROM airlines ORDER BY name";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            List<Airline> airlines = new ArrayList<>();
            while (rows.next()) {
                airlines.add(new Airline(rows.getLong("id"), rows.getString("iata_code"), rows.getString("name")));
            }
            return airlines;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load airlines", e);
        }
    }

    @Override
    public Optional<Airline> findByCode(String code) {
        String sql = "SELECT id, iata_code, name FROM airlines WHERE iata_code = ?";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, code);
            try (ResultSet row = statement.executeQuery()) {
                return row.next()
                        ? Optional.of(new Airline(row.getLong("id"), row.getString("iata_code"), row.getString("name")))
                        : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not look up airline " + code, e);
        }
    }
}
