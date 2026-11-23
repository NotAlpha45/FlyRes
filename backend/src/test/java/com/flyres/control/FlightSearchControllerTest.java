package com.flyres.control;

import static com.flyres.control.FlightSearchController.AIRLINE;
import static com.flyres.control.FlightSearchController.DATE;
import static com.flyres.control.FlightSearchController.DESTINATION;
import static com.flyres.control.FlightSearchController.ORIGIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flyres.common.exception.AccessDeniedException;
import com.flyres.common.exception.NotFoundException;
import com.flyres.common.exception.ValidationException;
import com.flyres.data.DatabaseManager;
import com.flyres.data.sqlite.SqliteAirlineRepository;
import com.flyres.data.sqlite.SqliteAirportRepository;
import com.flyres.data.sqlite.SqliteFlightRepository;
import com.flyres.data.sqlite.SqliteSeatRepository;
import com.flyres.domain.flight.FlightDetails;
import com.flyres.domain.flight.FlightStatus;
import com.flyres.domain.flight.FlightSummary;
import com.flyres.domain.flight.SeatAvailability;
import com.flyres.domain.flight.SearchCriteria;
import com.flyres.domain.user.Role;
import com.flyres.domain.user.User;
import com.flyres.domain.user.UserFactory;
import com.flyres.fixtures.FlightFixture;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FlightSearchControllerTest {

    private static final LocalDate TODAY = LocalDate.of(2030, 3, 10);
    private static final User CUSTOMER = user(Role.CUSTOMER, 1);
    private static final User AGENT = user(Role.AGENT, 2);
    private static final User ADMIN = user(Role.ADMIN, 3);

    private DatabaseManager database;
    private FlightSearchController search;

    private static User user(Role role, long id) {
        return UserFactory.create(role, id, role.name().toLowerCase() + "@example.com", "Test", role.displayName(),
                null, LocalDateTime.of(2030, 1, 1, 0, 0));
    }

    @BeforeEach
    void setUp() throws SQLException {
        database = DatabaseManager.forTesting();
        FlightFixture.insert(database.getConnection());
        search = new FlightSearchController(new SqliteFlightRepository(database), new SqliteSeatRepository(database),
                new SqliteAirportRepository(database), new SqliteAirlineRepository(database), FlightFixture.NOW);
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    private static List<String> numbers(List<FlightSummary> results) {
        return results.stream().map(summary -> summary.flight().getFlightNumber()).toList();
    }

    private static ValidationException assertInvalid(Runnable call) {
        return assertThrows(ValidationException.class, call::run);
    }

    // ---------- success ----------

    @Test
    void searchFlights_validRouteNoDate_returnsUpcomingSortedByDeparture() {
        List<FlightSummary> results = search.searchFlights(CUSTOMER, new SearchCriteria("YYC", "YVR", null, null));

        assertEquals(List.of("AC104", "AC108", "WS200"), numbers(results));
    }

    @Test
    void searchFlights_withDate_returnsOnlyThatDay() {
        List<FlightSummary> results = search.searchFlights(CUSTOMER,
                new SearchCriteria("YYC", "YVR", TODAY.plusDays(1), null));

        assertEquals(List.of("AC108", "WS200"), numbers(results));
    }

    @Test
    void searchFlights_today_excludesFlightsThatAlreadyDeparted() {
        List<FlightSummary> results = search.searchFlights(CUSTOMER, new SearchCriteria("YYC", "YVR", TODAY, null));

        // AC100 left at 08:00 and AC102 leaves exactly now; only the 18:00 flight remains.
        assertEquals(List.of("AC104"), numbers(results));
    }

    @Test
    void searchFlights_departedFlight_isExcluded() {
        List<FlightSummary> results = search.searchFlights(AGENT, new SearchCriteria("YYC", "YVR", null, null));

        assertTrue(numbers(results).stream().noneMatch(number -> number.equals("AC100") || number.equals("AC102")));
    }

    @Test
    void searchFlights_archivedFlight_isExcluded() {
        List<FlightSummary> results = search.searchFlights(CUSTOMER,
                new SearchCriteria("YYC", "YVR", TODAY.plusDays(2), null));

        assertTrue(results.isEmpty());
    }

    @Test
    void searchFlights_withAirline_returnsOnlyThatAirline() {
        List<FlightSummary> results = search.searchFlights(CUSTOMER, new SearchCriteria("YYC", "YVR", null, "AC"));

        assertEquals(List.of("AC104", "AC108"), numbers(results));
    }

    @Test
    void searchFlights_lowerCaseCodesWithSpaces_areNormalised() {
        List<FlightSummary> results = search.searchFlights(CUSTOMER,
                new SearchCriteria(" yyc ", "yyz", null, " ws "));

        assertEquals(List.of("WS300"), numbers(results));
    }

    @Test
    void searchFlights_blankAirline_meansAllAirlines() {
        List<FlightSummary> results = search.searchFlights(CUSTOMER, new SearchCriteria("YYC", "YVR", null, "  "));

        assertEquals(3, results.size());
    }

    @Test
    void searchFlights_availableSeats_capacityMinusConfirmed() {
        FlightSummary ac104 = search.searchFlights(CUSTOMER, new SearchCriteria("YYC", "YVR", TODAY, null)).get(0);

        assertEquals(3, ac104.availableSeats());
    }

    @Test
    void searchFlights_noMatches_returnsEmptyList() {
        assertTrue(search.searchFlights(CUSTOMER, new SearchCriteria("YVR", "YYC", null, null)).isEmpty());
    }

    // ---------- validation ----------

    @Test
    void searchFlights_missingOriginAndDestination_throwsValidationExceptionForBoth() {
        ValidationException e = assertInvalid(
                () -> search.searchFlights(CUSTOMER, new SearchCriteria(null, " ", null, null)));

        assertEquals(List.of(ORIGIN, DESTINATION), List.copyOf(e.getFieldErrors().keySet()));
    }

    @Test
    void searchFlights_sameOriginAndDestination_throwsValidationException() {
        ValidationException e = assertInvalid(
                () -> search.searchFlights(CUSTOMER, new SearchCriteria("YYC", "yyc", null, null)));

        assertEquals(List.of(DESTINATION), List.copyOf(e.getFieldErrors().keySet()));
    }

    @Test
    void searchFlights_unknownAirport_throwsValidationException() {
        ValidationException e = assertInvalid(
                () -> search.searchFlights(CUSTOMER, new SearchCriteria("XXX", "YVR", null, null)));

        assertEquals(List.of(ORIGIN), List.copyOf(e.getFieldErrors().keySet()));
    }

    @Test
    void searchFlights_pastDate_throwsValidationException() {
        ValidationException e = assertInvalid(
                () -> search.searchFlights(CUSTOMER, new SearchCriteria("YYC", "YVR", TODAY.minusDays(1), null)));

        assertEquals(List.of(DATE), List.copyOf(e.getFieldErrors().keySet()));
    }

    @Test
    void searchFlights_unknownAirline_throwsValidationException() {
        ValidationException e = assertInvalid(
                () -> search.searchFlights(CUSTOMER, new SearchCriteria("YYC", "YVR", null, "ZZ")));

        assertEquals(List.of(AIRLINE), List.copyOf(e.getFieldErrors().keySet()));
    }

    // ---------- roles ----------

    @Test
    void searchFlights_asAdmin_throwsAccessDeniedException() {
        assertThrows(AccessDeniedException.class,
                () -> search.searchFlights(ADMIN, new SearchCriteria("YYC", "YVR", null, null)));
    }

    @Test
    void searchFlights_noUser_throwsAccessDeniedException() {
        assertThrows(AccessDeniedException.class,
                () -> search.searchFlights(null, new SearchCriteria("YYC", "YVR", null, null)));
    }

    @Test
    void listAirportsAndAirlines_asCustomer_returnsAll() {
        assertEquals(3, search.listAirports(CUSTOMER).size());
        assertEquals(2, search.listAirlines(AGENT).size());
    }

    // ---------- getFlightDetails ----------

    @Test
    void getFlightDetails_scheduledFlight_returnsSeatsAndAvailability() {
        FlightDetails details = search.getFlightDetails(CUSTOMER, FlightFixture.AC104);

        assertEquals("AC104", details.flight().getFlightNumber());
        assertEquals(2, details.seatRows());
        assertEquals(2, details.seatsPerRow());
        assertEquals(List.of("1A", "1B", "2A", "2B"), details.seats().stream().map(s -> s.seat().label()).toList());
        assertEquals(List.of(true, false, false, false), details.seats().stream().map(SeatAvailability::taken).toList());
        assertEquals(3, details.availableCount());
    }

    @Test
    void getFlightDetails_asAgent_returnsDetails() {
        assertEquals(6, search.getFlightDetails(AGENT, FlightFixture.WS200).seats().size());
    }

    @Test
    void getFlightDetails_unknownId_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> search.getFlightDetails(CUSTOMER, 999));
    }

    @Test
    void getFlightDetails_archivedFlightAsCustomer_throwsNotFoundException() {
        NotFoundException e = assertThrows(NotFoundException.class,
                () -> search.getFlightDetails(CUSTOMER, FlightFixture.AC106_ARCHIVED));

        assertEquals(FlightSearchController.FLIGHT_UNAVAILABLE, e.getMessage());
    }

    @Test
    void getFlightDetails_departedFlightAsAgent_throwsNotFoundException() {
        // Flight 2 (AC102) departs exactly at the fixture's "now".
        assertThrows(NotFoundException.class, () -> search.getFlightDetails(AGENT, 2));
    }

    @Test
    void getFlightDetails_archivedFlightAsAdmin_returnsDetails() {
        assertEquals(FlightStatus.ARCHIVED,
                search.getFlightDetails(ADMIN, FlightFixture.AC106_ARCHIVED).flight().getStatus());
    }

    @Test
    void getFlightDetails_noUser_throwsAccessDeniedException() {
        assertThrows(AccessDeniedException.class, () -> search.getFlightDetails(null, FlightFixture.AC104));
    }

    @Test
    void listAirports_asAdmin_throwsAccessDeniedException() {
        assertThrows(AccessDeniedException.class, () -> search.listAirports(ADMIN));
        assertThrows(AccessDeniedException.class, () -> search.listAirlines(ADMIN));
    }
}
