package com.flyres.domain.flight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class FlightTest {

    private static final Clock NOON = Clock.fixed(Instant.parse("2030-03-10T12:00:00Z"), ZoneOffset.UTC);
    private static final Airline AIRLINE = new Airline(1, "AC", "Air Canada");
    private static final Aircraft AIRCRAFT = new Aircraft(1, AIRLINE, "C-FDAC", "Airbus A220-300", 20, 5);
    private static final Route ROUTE = new Route(1,
            new Airport(1, "YYC", "Calgary International", "Calgary", "Canada"),
            new Airport(2, "YVR", "Vancouver International", "Vancouver", "Canada"));

    private static Flight departingAt(LocalDateTime departure) {
        return new Flight(1, "AC211", ROUTE, AIRCRAFT, departure, departure.plusMinutes(90), 18900,
                FlightStatus.SCHEDULED);
    }

    @Test
    void isDeparted_departureLaterToday_false() {
        assertFalse(departingAt(LocalDateTime.of(2030, 3, 10, 12, 1)).isDeparted(NOON));
    }

    @Test
    void isDeparted_departureExactlyNow_true() {
        assertTrue(departingAt(LocalDateTime.of(2030, 3, 10, 12, 0)).isDeparted(NOON));
    }

    @Test
    void isDeparted_departureEarlier_true() {
        assertTrue(departingAt(LocalDateTime.of(2030, 3, 10, 8, 0)).isDeparted(NOON));
    }

    @Test
    void duration_and_airline_derivedFromScheduleAndAircraft() {
        Flight flight = departingAt(LocalDateTime.of(2030, 3, 11, 7, 0));

        assertEquals(Duration.ofMinutes(90), flight.duration());
        assertEquals(AIRLINE, flight.airline());
    }

    @Test
    void constructor_arrivalNotAfterDeparture_throws() {
        LocalDateTime time = LocalDateTime.of(2030, 3, 11, 7, 0);
        assertThrows(IllegalArgumentException.class,
                () -> new Flight(1, "AC211", ROUTE, AIRCRAFT, time, time, 18900, FlightStatus.SCHEDULED));
    }

    @Test
    void aircraftCapacity_isRowsTimesSeatsPerRow() {
        assertEquals(100, AIRCRAFT.capacity());
    }

    @Test
    void route_sameAirportTwice_throws() {
        Airport yyc = ROUTE.origin();
        assertThrows(IllegalArgumentException.class, () -> new Route(2, yyc, yyc));
    }
}
