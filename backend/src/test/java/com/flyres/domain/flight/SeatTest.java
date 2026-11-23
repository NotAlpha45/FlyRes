package com.flyres.domain.flight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SeatTest {

    private static Seat seat(String label) {
        return new Seat(1, 1, label);
    }

    @Test
    void rowLetterAndColumn_parsedFromLabel() {
        Seat seat = seat("12C");

        assertEquals(12, seat.row());
        assertEquals('C', seat.letter());
        assertEquals(2, seat.column());
        assertEquals(9, seat("9F").row());
    }

    @Test
    void mapOrder_isNumericByRowThenLetter() {
        List<String> sorted = List.of("10A", "2B", "1B", "2A", "1A").stream()
                .map(SeatTest::seat).sorted(Seat.MAP_ORDER).map(Seat::label).toList();

        assertEquals(List.of("1A", "1B", "2A", "2B", "10A"), sorted);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "A1", "0A", "12", "1K", "100A", "1a"})
    void constructor_invalidLabel_throws(String label) {
        assertThrows(IllegalArgumentException.class, () -> seat(label));
    }

    @Test
    void flightDetails_ofSeats_derivesLayoutAndOrderAndCounts() {
        Airline airline = new Airline(1, "AC", "Air Canada");
        Flight flight = new Flight(1, "AC211",
                new Route(1, new Airport(1, "YYC", "Calgary", "Calgary", "Canada"),
                        new Airport(2, "YVR", "Vancouver", "Vancouver", "Canada")),
                new Aircraft(1, airline, "C-FDAC", "A220", 2, 3),
                LocalDateTime.of(2030, 3, 11, 7, 0), LocalDateTime.of(2030, 3, 11, 8, 30), 18900,
                FlightStatus.SCHEDULED);
        List<SeatAvailability> seats = List.of(
                new SeatAvailability(seat("2C"), false), new SeatAvailability(seat("1A"), true),
                new SeatAvailability(seat("1C"), false), new SeatAvailability(seat("2A"), false),
                new SeatAvailability(seat("1B"), true), new SeatAvailability(seat("2B"), false));

        FlightDetails details = FlightDetails.of(flight, seats);

        assertEquals(2, details.seatRows());
        assertEquals(3, details.seatsPerRow());
        assertEquals(List.of("1A", "1B", "1C", "2A", "2B", "2C"),
                details.seats().stream().map(s -> s.seat().label()).toList());
        assertEquals(4, details.availableCount());
        assertFalse(details.isSoldOut());
        assertTrue(FlightDetails.of(flight, List.of(new SeatAvailability(seat("1A"), true))).isSoldOut());
    }
}
