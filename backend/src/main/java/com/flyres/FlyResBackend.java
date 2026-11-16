package com.flyres;

import com.flyres.common.security.PasswordHasher;
import com.flyres.control.AuthenticationController;
import com.flyres.data.DatabaseManager;
import com.flyres.data.sqlite.SqliteUserRepository;
import com.flyres.repository.UserRepository;
import java.time.Clock;

/**
 * Facade over the backend and the UI's only entry point.
 * <p>
 * Wires the SQLite repository implementations into the control classes through their constructors.
 * Controls are added here as their issues are implemented.
 */
public final class FlyResBackend implements AutoCloseable {

    private final DatabaseManager database;
    private final AuthenticationController authentication;

    /** Builds the backend on the given database with the system clock. */
    public FlyResBackend(DatabaseManager database) {
        this(database, Clock.systemDefaultZone());
    }

    /** Builds the backend on the given database; tests pass {@link DatabaseManager#forTesting()} and a fixed clock. */
    public FlyResBackend(DatabaseManager database, Clock clock) {
        this.database = database;

        UserRepository users = new SqliteUserRepository(database);

        this.authentication = new AuthenticationController(users, new PasswordHasher(), clock);
    }

    /** Builds the backend on the shared production database. */
    public static FlyResBackend create() {
        return new FlyResBackend(DatabaseManager.getInstance());
    }

    /** Login and customer registration. */
    public AuthenticationController authentication() {
        return authentication;
    }

    /** Releases the database connection. */
    @Override
    public void close() {
        database.close();
    }
}
