package com.flyres.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton owner of the application's single SQLite connection.
 * <p>
 * The production database lives at {@code ~/.flyres/flyres.db}. When the file does not exist yet,
 * it is created from {@code db/schema.sql} and populated from {@code db/seed.sql}.
 */
public final class DatabaseManager implements AutoCloseable {

    private static final Path DEFAULT_DATABASE = Path.of(System.getProperty("user.home"), ".flyres", "flyres.db");
    private static final String SCHEMA_SCRIPT = "/db/schema.sql";
    private static final String SEED_SCRIPT = "/db/seed.sql";

    /** How long a statement waits for another process's lock before failing with SQLITE_BUSY. */
    static final int BUSY_TIMEOUT_MILLIS = 3000;

    private static DatabaseManager instance;

    private final Connection connection;

    private DatabaseManager(String jdbcUrl) {
        try {
            this.connection = DriverManager.getConnection(jdbcUrl);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA busy_timeout = " + BUSY_TIMEOUT_MILLIS);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not open database " + jdbcUrl, e);
        }
    }

    /** Returns the shared production database, creating and seeding it on first use. */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = openFile(DEFAULT_DATABASE);
        }
        return instance;
    }

    /** Returns a new, independent in-memory database with the schema applied but no seed data. */
    public static DatabaseManager forTesting() {
        DatabaseManager database = new DatabaseManager("jdbc:sqlite::memory:");
        database.runScript(SCHEMA_SCRIPT);
        return database;
    }

    /** Returns a new, independent in-memory database with the schema and the demo seed data. */
    public static DatabaseManager forTestingWithSeedData() {
        DatabaseManager database = forTesting();
        database.runScript(SEED_SCRIPT);
        return database;
    }

    /**
     * Opens the database file, creating and seeding it in one transaction if it does not exist.
     * If creation fails, the half-built file is deleted so the next launch starts clean.
     */
    static DatabaseManager openFile(Path file) {
        boolean isNew = Files.notExists(file);
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create database directory for " + file, e);
        }
        DatabaseManager database = new DatabaseManager("jdbc:sqlite:" + file);
        if (!isNew) {
            database.checkReadable(file);
        }
        if (isNew) {
            try {
                new TransactionRunner(database).inTransaction(() -> {
                    database.runScript(SCHEMA_SCRIPT);
                    database.runScript(SEED_SCRIPT);
                });
            } catch (RuntimeException e) {
                database.close();
                deleteQuietly(file, e);
                throw e;
            }
        }
        return database;
    }

    /**
     * Reads the schema once so a corrupt or non-SQLite file fails at startup, not on the first click.
     * SQLite opens such files lazily and only reports "file is not a database" on the first query.
     */
    private void checkReadable(Path file) {
        try (Statement statement = connection.createStatement()) {
            statement.executeQuery("SELECT count(*) FROM sqlite_master").close();
        } catch (SQLException e) {
            close();
            throw new IllegalStateException("Could not read database " + file, e);
        }
    }

    private static void deleteQuietly(Path file, RuntimeException cause) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            cause.addSuppressed(e);
        }
    }

    /** Returns the shared connection. Callers must not close it. */
    public Connection getConnection() {
        return connection;
    }

    @Override
    public void close() {
        synchronized (DatabaseManager.class) {
            if (instance == this) {
                instance = null;
            }
        }
        try {
            connection.close();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not close database", e);
        }
    }

    private void runScript(String resource) {
        try (Statement statement = connection.createStatement()) {
            for (String sql : readStatements(resource)) {
                statement.execute(sql);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not run " + resource, e);
        }
    }

    /**
     * Splits a script into statements on {@code ;}, skipping {@code --} comment lines.
     * Triggers and semicolons inside string literals are not supported.
     */
    private static List<String> readStatements(String resource) {
        String script;
        try (InputStream in = DatabaseManager.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + resource);
            }
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + resource, e);
        }

        StringBuilder withoutComments = new StringBuilder();
        for (String line : script.split("\\R")) {
            if (!line.strip().startsWith("--")) {
                withoutComments.append(line).append('\n');
            }
        }

        List<String> statements = new ArrayList<>();
        for (String sql : withoutComments.toString().split(";")) {
            if (!sql.isBlank()) {
                statements.add(sql.strip());
            }
        }
        return statements;
    }
}
