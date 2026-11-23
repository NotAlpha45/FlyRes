package com.flyres.data.sqlite;

import com.flyres.common.exception.DataAccessException;
import com.flyres.data.DatabaseManager;
import com.flyres.domain.flight.Airport;
import com.flyres.repository.AirportRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQLite implementation of {@link AirportRepository}. */
public final class SqliteAirportRepository implements AirportRepository {

    private static final String COLUMNS = "id, iata_code, name, city, country";

    private final DatabaseManager database;

    public SqliteAirportRepository(DatabaseManager database) {
        this.database = database;
    }

    @Override
    public List<Airport> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM airports ORDER BY city, iata_code";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            List<Airport> airports = new ArrayList<>();
            while (rows.next()) {
                airports.add(toAirport(rows));
            }
            return airports;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load airports", e);
        }
    }

    @Override
    public Optional<Airport> findByCode(String code) {
        String sql = "SELECT " + COLUMNS + " FROM airports WHERE iata_code = ?";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, code);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? Optional.of(toAirport(row)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not look up airport " + code, e);
        }
    }

    private static Airport toAirport(ResultSet row) throws SQLException {
        return new Airport(row.getLong("id"), row.getString("iata_code"), row.getString("name"),
                row.getString("city"), row.getString("country"));
    }
}
