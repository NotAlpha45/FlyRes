package com.flyres.data.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flyres.data.DatabaseManager;
import com.flyres.domain.flight.Flight;
import com.flyres.domain.flight.FlightStatus;
import com.flyres.domain.flight.FlightSummary;
import com.flyres.fixtures.FlightFixture;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqliteFlightRepositoryTest {

    private static final LocalDateTime START = LocalDateTime.of(2030, 3, 1, 0, 0);

    private DatabaseManager database;
    private SqliteFlightRepository flights;

    @BeforeEach
    void setUp() throws SQLException {
        database = DatabaseManager.forTesting();
        FlightFixture.insert(database.getConnection());
        flights = new SqliteFlightRepository(database);
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    private static List<String> numbers(List<FlightSummary> results) {
        return results.stream().map(summary -> summary.flight().getFlightNumber()).toList();
    }

    @Test
    void search_route_returnsScheduledSortedByDeparture() {
        List<FlightSummary> results = flights.search("YYC", "YVR", START, null, null);

        assertEquals(List.of("AC100", "AC102", "AC104", "AC108", "WS200"), numbers(results));
    }

    @Test
    void search_excludesArchived() {
        List<FlightSummary> results = flights.search("YYC", "YVR", START, null, null);

        assertTrue(results.stream().noneMatch(summary -> summary.flight().getStatus() == FlightStatus.ARCHIVED));
    }

    @Test
    void search_dateRange_fromInclusiveToExclusive() {
        List<FlightSummary> results = flights.search("YYC", "YVR",
                LocalDateTime.of(2030, 3, 11, 0, 0), LocalDateTime.of(2030, 3, 12, 0, 0), null);

        assertEquals(List.of("AC108", "WS200"), numbers(results));
    }

    @Test
    void search_airline_returnsOnlyThatAirline() {
        List<FlightSummary> results = flights.search("YYC", "YVR", START, null, "WS");

        assertEquals(List.of("WS200"), numbers(results));
    }

    @Test
    void search_availableSeats_capacityMinusConfirmedOnly() {
        FlightSummary ac104 = flights.search("YYC", "YVR", START, null, null).stream()
                .filter(summary -> summary.flight().getId() == FlightFixture.AC104)
                .findFirst().orElseThrow();

        // 4 seats, 1A CONFIRMED, 1B CANCELLED.
        assertEquals(3, ac104.availableSeats());
    }

    @Test
    void search_cancellingReservation_freesSeat() throws SQLException {
        try (Statement statement = database.getConnection().createStatement()) {
            statement.execute("UPDATE reservations SET status = 'CANCELLED', cancelled_at = '2030-03-05T00:00:00' "
                    + "WHERE confirmation_code = 'CONF01'");
        }

        FlightSummary ac104 = flights.search("YYC", "YVR", START, null, "AC").stream()
                .filter(summary -> summary.flight().getId() == FlightFixture.AC104)
                .findFirst().orElseThrow();

        assertEquals(4, ac104.availableSeats());
    }

    @Test
    void findById_mapsEveryField() {
        Flight flight = flights.findById(FlightFixture.WS300).orElseThrow();

        assertEquals("WS300", flight.getFlightNumber());
        assertEquals("YYC", flight.getRoute().origin().code());
        assertEquals("Toronto", flight.getRoute().destination().city());
        assertEquals("WS", flight.airline().code());
        assertEquals(6, flight.getAircraft().capacity());
        assertEquals(LocalDateTime.of(2030, 3, 11, 7, 0), flight.getDepartureTime());
        assertEquals(LocalDateTime.of(2030, 3, 11, 13, 0), flight.getArrivalTime());
        assertEquals(32900, flight.getPriceCents());
        assertEquals(FlightStatus.SCHEDULED, flight.getStatus());
    }

    @Test
    void findById_archivedFlight_isReturned() {
        assertEquals(FlightStatus.ARCHIVED, flights.findById(FlightFixture.AC106_ARCHIVED).orElseThrow().getStatus());
    }

    @Test
    void findById_unknown_isEmpty() {
        assertTrue(flights.findById(999).isEmpty());
    }

    @Test
    void airportsAndAirlines_lookups() {
        SqliteAirportRepository airports = new SqliteAirportRepository(database);
        SqliteAirlineRepository airlines = new SqliteAirlineRepository(database);

        assertEquals(List.of("YYC", "YYZ", "YVR"), airports.findAll().stream().map(a -> a.code()).toList());
        assertEquals("Vancouver", airports.findByCode("YVR").orElseThrow().city());
        assertTrue(airports.findByCode("XXX").isEmpty());
        assertEquals(List.of("Air Canada", "WestJet"), airlines.findAll().stream().map(a -> a.name()).toList());
        assertTrue(airlines.findByCode("ZZ").isEmpty());
    }
}
