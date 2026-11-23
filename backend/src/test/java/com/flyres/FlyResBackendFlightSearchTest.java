package com.flyres;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.flyres.data.DatabaseManager;
import com.flyres.domain.flight.FlightSummary;
import com.flyres.domain.flight.SearchCriteria;
import com.flyres.domain.user.Role;
import com.flyres.domain.user.User;
import com.flyres.domain.user.UserFactory;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Flight search through the facade on the real seed data. Seed dates are relative to the moment the
 * database is created, so this test uses the system clock, like the app does.
 */
class FlyResBackendFlightSearchTest {

    private static final User MAYA = UserFactory.create(Role.CUSTOMER, 3, "maya@example.com", "Maya", "Patel",
            null, LocalDateTime.now());

    private FlyResBackend backend;

    @BeforeEach
    void setUp() {
        backend = new FlyResBackend(DatabaseManager.forTestingWithSeedData(), Clock.systemDefaultZone());
    }

    @AfterEach
    void tearDown() {
        backend.close();
    }

    private List<FlightSummary> search(String from, String to, String airline) {
        return backend.flightSearch().searchFlights(MAYA, new SearchCriteria(from, to, null, airline));
    }

    @Test
    void seed_yycToYvr_listsBothUpcomingFlightsInOrder() {
        List<FlightSummary> results = search("YYC", "YVR", null);

        assertEquals(List.of("AC211", "AC211"), results.stream().map(s -> s.flight().getFlightNumber()).toList());
        assertEquals(List.of(18900L, 17900L), results.stream().map(s -> s.flight().getPriceCents()).toList());
    }

    @Test
    void seed_yycToYyz_excludesArchivedF8603AndCountsConfirmedSeats() {
        List<FlightSummary> results = search("YYC", "YYZ", null);

        assertEquals(List.of("WS101", "F8601", "WS101"), results.stream().map(s -> s.flight().getFlightNumber()).toList());
        // Boeing 737-800 has 150 seats; Maya and Omar hold 12A and 12B on the first WS101.
        assertEquals(148, results.get(0).availableSeats());
    }

    @Test
    void seed_yycToYyzOnFlair_returnsOnlyF8601() {
        List<FlightSummary> results = search("YYC", "YYZ", "F8");

        assertEquals(List.of("F8601"), results.stream().map(s -> s.flight().getFlightNumber()).toList());
    }
}
