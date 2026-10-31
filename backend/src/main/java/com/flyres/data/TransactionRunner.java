package com.flyres.data;

import com.flyres.common.exception.DataAccessException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * Runs a unit of work in a single database transaction: commit if it returns normally,
 * roll back if it throws. A call made inside another transaction joins the outer one.
 */
public final class TransactionRunner {

    private final DatabaseManager database;

    public TransactionRunner(DatabaseManager database) {
        this.database = database;
    }

    /** Runs {@code work} in a transaction and returns its result. */
    public <T> T inTransaction(Supplier<T> work) {
        Connection connection = database.getConnection();
        try {
            if (!connection.getAutoCommit()) {
                return work.get();
            }
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new DataAccessException("Could not start transaction", e);
        }

        try {
            T result = work.get();
            connection.commit();
            return result;
        } catch (RuntimeException | Error e) {
            rollback(connection, e);
            throw e;
        } catch (SQLException e) {
            DataAccessException failure = new DataAccessException("Could not commit transaction", e);
            rollback(connection, failure);
            throw failure;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                throw new DataAccessException("Could not end transaction", e);
            }
        }
    }

    /** Runs {@code work} in a transaction. */
    public void inTransaction(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }

    private static void rollback(Connection connection, Throwable cause) {
        try {
            connection.rollback();
        } catch (SQLException e) {
            cause.addSuppressed(e);
        }
    }
}
