package com.flyres.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseManagerTest {

    private static final List<String> TABLES = List.of(
            "users", "airports", "airlines", "aircraft", "routes", "flights", "seats",
            "reservations", "payments", "promotions", "promotion_deliveries");

    @TempDir
    Path tempDir;

    @Test
    void forTesting_newDatabase_hasEveryTableAndNoRows() throws SQLException {
        try (DatabaseManager database = DatabaseManager.forTesting()) {
            for (String table : TABLES) {
                assertEquals(0, TestRows.count(database.getConnection(), table), table);
            }
        }
    }

    @Test
    void openFile_firstLaunch_createsFileWithSeedRows() throws SQLException {
        Path file = tempDir.resolve("flyres.db");

        try (DatabaseManager database = DatabaseManager.openFile(file)) {
            assertTrue(Files.exists(file));
            assertEquals(4, TestRows.count(database.getConnection(), "users"));
            assertTrue(TestRows.count(database.getConnection(), "flights") > 0);
        }
    }

    @Test
    void openFile_secondLaunch_doesNotSeedAgain() throws SQLException {
        Path file = tempDir.resolve("flyres.db");
        try (DatabaseManager first = DatabaseManager.openFile(file);
             Statement statement = first.getConnection().createStatement()) {
            statement.execute("UPDATE users SET first_name = 'Changed' WHERE id = 1");
        }

        try (DatabaseManager second = DatabaseManager.openFile(file);
             Statement statement = second.getConnection().createStatement();
             ResultSet result = statement.executeQuery("SELECT first_name FROM users WHERE id = 1")) {
            assertEquals("Changed", result.getString(1));
            assertEquals(4, TestRows.count(second.getConnection(), "users"));
        }
    }

    @Test
    void openFile_fileDeleted_recreatesAndReseeds() throws Exception {
        Path file = tempDir.resolve("flyres.db");
        try (DatabaseManager first = DatabaseManager.openFile(file);
             Statement statement = first.getConnection().createStatement()) {
            statement.execute("DELETE FROM promotions WHERE id = 7");
        }
        Files.delete(file);

        try (DatabaseManager second = DatabaseManager.openFile(file)) {
            assertEquals(7, TestRows.count(second.getConnection(), "promotions"));
        }
    }

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
