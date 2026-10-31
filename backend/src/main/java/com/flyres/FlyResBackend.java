package com.flyres;

import com.flyres.data.DatabaseManager;

/**
 * Facade over the backend and the UI's only entry point.
 * <p>
 * Wires the SQLite repository implementations into the control classes through their constructors.
 * Controls are added here as their issues are implemented.
 */
public final class FlyResBackend implements AutoCloseable {

    private final DatabaseManager database;

    /** Builds the backend on the given database; tests pass {@link DatabaseManager#forTesting()}. */
    public FlyResBackend(DatabaseManager database) {
        this.database = database;
    }

    /** Builds the backend on the shared production database. */
    public static FlyResBackend create() {
        return new FlyResBackend(DatabaseManager.getInstance());
    }

    /** Releases the database connection. */
    @Override
    public void close() {
        database.close();
    }
}
