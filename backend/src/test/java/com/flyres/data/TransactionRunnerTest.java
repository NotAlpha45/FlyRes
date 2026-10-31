package com.flyres.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionRunnerTest {

    private DatabaseManager database;
    private Connection connection;
    private TransactionRunner transactions;

    @BeforeEach
    void setUp() throws SQLException {
        database = DatabaseManager.forTesting();
        connection = database.getConnection();
        transactions = new TransactionRunner(database);
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE probe (id INTEGER PRIMARY KEY)");
        }
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    @Test
    void inTransaction_workSucceeds_commitsAndReturnsResult() throws SQLException {
        String result = transactions.inTransaction(() -> {
            insertProbe(1);
            return "done";
        });

        assertEquals("done", result);
        assertEquals(1, TestRows.count(connection, "probe"));
        assertTrue(connection.getAutoCommit());
    }

    @Test
    void inTransaction_workThrows_rollsBackAndRethrows() throws SQLException {
        IllegalStateException failure = new IllegalStateException("payment declined");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> transactions.inTransaction(() -> {
                    insertProbe(1);
                    throw failure;
                }));

        assertSame(failure, thrown);
        assertEquals(0, TestRows.count(connection, "probe"));
        assertTrue(connection.getAutoCommit());
    }

    @Test
    void inTransaction_nestedCallFails_rollsBackOuterWorkToo() throws SQLException {
        assertThrows(IllegalStateException.class, () -> transactions.inTransaction(() -> {
            insertProbe(1);
            transactions.inTransaction(() -> {
                insertProbe(2);
                throw new IllegalStateException("inner failure");
            });
        }));

        assertEquals(0, TestRows.count(connection, "probe"));
    }

    private void insertProbe(int id) {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO probe (id) VALUES (?)")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
