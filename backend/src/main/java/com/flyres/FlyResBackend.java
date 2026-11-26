package com.flyres;

import com.flyres.common.security.PasswordHasher;
import com.flyres.control.AuthenticationController;
import com.flyres.control.FlightSearchController;
import com.flyres.control.PaymentController;
import com.flyres.data.DatabaseManager;
import com.flyres.data.sqlite.SqliteAirlineRepository;
import com.flyres.data.sqlite.SqliteAirportRepository;
import com.flyres.data.sqlite.SqliteFlightRepository;
import com.flyres.data.sqlite.SqlitePaymentRepository;
import com.flyres.data.sqlite.SqliteSeatRepository;
import com.flyres.data.sqlite.SqliteUserRepository;
import com.flyres.repository.AirlineRepository;
import com.flyres.repository.AirportRepository;
import com.flyres.repository.FlightRepository;
import com.flyres.repository.SeatRepository;
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
    private final FlightSearchController flightSearch;
    private final PaymentController payments;

    /** Builds the backend on the given database with the system clock. */
    public FlyResBackend(DatabaseManager database) {
        this(database, Clock.systemDefaultZone());
    }

    /** Builds the backend on the given database; tests pass {@link DatabaseManager#forTesting()} and a fixed clock. */
    public FlyResBackend(DatabaseManager database, Clock clock) {
        this.database = database;

        UserRepository users = new SqliteUserRepository(database);
        FlightRepository flights = new SqliteFlightRepository(database);
        SeatRepository seats = new SqliteSeatRepository(database);
        AirportRepository airports = new SqliteAirportRepository(database);
        AirlineRepository airlines = new SqliteAirlineRepository(database);

        this.authentication = new AuthenticationController(users, new PasswordHasher(), clock);
        this.flightSearch = new FlightSearchController(flights, seats, airports, airlines, clock);
        this.payments = new PaymentController(new SqlitePaymentRepository(database), clock);
    }

    /** Builds the backend on the shared production database. */
    public static FlyResBackend create() {
        return new FlyResBackend(DatabaseManager.getInstance());
    }

    /** Login and customer registration. */
    public AuthenticationController authentication() {
        return authentication;
    }

    /** Flight search and flight details (seat map). */
    public FlightSearchController flightSearch() {
        return flightSearch;
    }

    /** Simulated card payments. */
    public PaymentController payments() {
        return payments;
    }

    /** Releases the database connection. */
    @Override
    public void close() {
        database.close();
    }
}
