package com.flyres.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;

class DatabaseManagerTest {

    @Test
    void forTesting_newDatabase_foreignKeysEnabled() throws SQLException {
        try (DatabaseManager database = DatabaseManager.forTesting();
             Statement statement = database.getConnection().createStatement();
             ResultSet result = statement.executeQuery("PRAGMA foreign_keys")) {
            assertEquals(1, result.getInt(1));
        }
    }

    @Test
    void forTesting_calledTwice_databasesAreIndependent() throws SQLException {
        try (DatabaseManager first = DatabaseManager.forTesting();
             DatabaseManager second = DatabaseManager.forTesting()) {
            try (Statement statement = first.getConnection().createStatement()) {
                statement.execute("CREATE TABLE probe (id INTEGER PRIMARY KEY)");
            }
            try (Statement statement = second.getConnection().createStatement();
                 ResultSet result = statement.executeQuery(
                         "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'probe'")) {
                assertFalse(result.next());
            }
        }
    }
}
