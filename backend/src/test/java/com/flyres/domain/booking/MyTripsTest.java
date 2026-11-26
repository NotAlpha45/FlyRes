package com.flyres.domain.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flyres.domain.flight.Aircraft;
import com.flyres.domain.flight.Airline;
import com.flyres.domain.flight.Airport;
import com.flyres.domain.flight.Flight;
import com.flyres.domain.flight.FlightStatus;
import com.flyres.domain.flight.Route;
import com.flyres.domain.flight.Seat;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class MyTripsTest {

    /** "Now" is 2030-03-10 12:00. */
    private static final Clock NOW = Clock.fixed(Instant.parse("2030-03-10T12:00:00Z"), ZoneOffset.UTC);
    private static final Aircraft AIRCRAFT = new Aircraft(1, new Airline(1, "AC", "Air Canada"), "C-TEST", "Jet", 2, 2);
    private static final Route ROUTE = new Route(1,
            new Airport(1, "YYC", "Calgary International", "Calgary", "Canada"),
            new Airport(2, "YVR", "Vancouver International", "Vancouver", "Canada"));
    private static final LocalDateTime BOOKED = LocalDateTime.of(2030, 3, 1, 9, 0);

    private static long nextId = 1;

    private static ReservationSummary trip(String code, LocalDateTime departure, FlightStatus flightStatus,
                                           ReservationStatus stored, LocalDateTime cancelledAt) {
        long id = nextId++;
        Flight flight = new Flight(id, "AC" + id, ROUTE, AIRCRAFT, departure, departure.plusMinutes(90), 18900,
                flightStatus);
        Reservation reservation = Reservation.restore(id, code, 1, new Seat(id, id, "1A"), flight, 18900, stored,
                BOOKED, cancelledAt);
        return reservation.summaryAt(NOW);
    }

    private static List<String> codes(List<ReservationSummary> trips) {
        return trips.stream().map(t -> t.reservation().getConfirmationCode()).toList();
    }

    @Test
    void of_groupsByEffectiveStatusAndSortsEachGroup() {
        ReservationSummary later = trip("LATER2", LocalDateTime.of(2030, 3, 20, 8, 0), FlightStatus.SCHEDULED,
                ReservationStatus.CONFIRMED, null);
        ReservationSummary sooner = trip("SOON22", LocalDateTime.of(2030, 3, 11, 8, 0), FlightStatus.SCHEDULED,
                ReservationStatus.CONFIRMED, null);
        ReservationSummary oldTrip = trip("OLDTR2", LocalDateTime.of(2030, 2, 1, 8, 0), FlightStatus.SCHEDULED,
                ReservationStatus.CONFIRMED, null);
        ReservationSummary recentTrip = trip("RECNT2", LocalDateTime.of(2030, 3, 9, 8, 0), FlightStatus.SCHEDULED,
                ReservationStatus.CONFIRMED, null);
        ReservationSummary cancelledFirst = trip("CANCA2", LocalDateTime.of(2030, 3, 25, 8, 0),
                FlightStatus.SCHEDULED, ReservationStatus.CANCELLED, LocalDateTime.of(2030, 3, 2, 9, 0));
        ReservationSummary cancelledLast = trip("CANCB2", LocalDateTime.of(2030, 3, 26, 8, 0),
                FlightStatus.SCHEDULED, ReservationStatus.CANCELLED, LocalDateTime.of(2030, 3, 8, 9, 0));

        MyTrips trips = MyTrips.of(List.of(later, oldTrip, cancelledFirst, sooner, recentTrip, cancelledLast));

        assertEquals(List.of("SOON22", "LATER2"), codes(trips.upcoming()));
        assertEquals(List.of("RECNT2", "OLDTR2"), codes(trips.past()));
        assertEquals(List.of("CANCB2", "CANCA2"), codes(trips.cancelled()));
        assertFalse(trips.isEmpty());
    }

    @Test
    void summary_departureExactlyNow_isCompleted() {
        ReservationSummary trip = trip("NOWDEP", LocalDateTime.of(2030, 3, 10, 12, 0), FlightStatus.SCHEDULED,
                ReservationStatus.CONFIRMED, null);

        assertEquals(ReservationStatus.COMPLETED, trip.status());
        assertEquals(ReservationStatus.CONFIRMED, trip.reservation().getStatus());
    }

    @Test
    void summary_cancelledOnArchivedFlight_isFlightRemoved() {
        ReservationSummary removed = trip("ARCH22", LocalDateTime.of(2030, 3, 20, 8, 0), FlightStatus.ARCHIVED,
                ReservationStatus.CANCELLED, LocalDateTime.of(2030, 3, 5, 9, 0));
        ReservationSummary byCustomer = trip("SELF22", LocalDateTime.of(2030, 3, 20, 8, 0), FlightStatus.SCHEDULED,
                ReservationStatus.CANCELLED, LocalDateTime.of(2030, 3, 5, 9, 0));

        assertTrue(removed.isFlightRemoved());
        assertFalse(byCustomer.isFlightRemoved());
    }

    @Test
    void of_noReservations_isEmpty() {
        assertTrue(MyTrips.of(List.of()).isEmpty());
    }
}
