package com.flyres.domain.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.flyres.data.DatabaseManager;
import com.flyres.data.sqlite.SqliteFlightRepository;
import com.flyres.data.sqlite.SqliteSeatRepository;
import com.flyres.domain.flight.Flight;
import com.flyres.domain.flight.Seat;
import com.flyres.fixtures.FlightFixture;
import java.sql.SQLException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReservationTest {

    private DatabaseManager database;
    private Flight flight;
    private Seat seat;

    @BeforeEach
    void setUp() throws SQLException {
        database = DatabaseManager.forTesting();
        FlightFixture.insert(database.getConnection());
        flight = new SqliteFlightRepository(database).findById(FlightFixture.WS200).orElseThrow();
        seat = new SqliteSeatRepository(database).findByFlight(FlightFixture.WS200).get(0).seat();
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    @Test
    void stateTransitions_confirmThenCancel() {
        LocalDateTime created = LocalDateTime.of(2030, 3, 10, 12, 0);
        Reservation reservation = Reservation.pending(1, seat, flight, 15900);

        reservation.confirm("ABC234", created);
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(created, reservation.getCreatedAt().orElseThrow());
        reservation.cancel(created.plusHours(1));

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(created.plusHours(1), reservation.getCancelledAt().orElseThrow());
    }

    @Test
    void cancel_pending_throws() {
        Reservation reservation = Reservation.pending(1, seat, flight, 15900);

        assertThrows(IllegalStateException.class, () -> reservation.cancel(FlightFixture.NOW.instant()
                .atZone(FlightFixture.NOW.getZone()).toLocalDateTime()));
    }

    @Test
    void confirm_twice_throws() {
        Reservation reservation = Reservation.pending(1, seat, flight, 15900);
        reservation.confirm("ABC234", LocalDateTime.of(2030, 3, 10, 12, 0));

        assertThrows(IllegalStateException.class,
                () -> reservation.confirm("DEF567", LocalDateTime.of(2030, 3, 10, 12, 1)));
    }

    @Test
    void cancel_cancelled_throws() {
        Reservation reservation = Reservation.pending(1, seat, flight, 15900);
        LocalDateTime now = LocalDateTime.of(2030, 3, 10, 12, 0);
        reservation.confirm("ABC234", now);
        reservation.cancel(now.plusMinutes(1));

        assertThrows(IllegalStateException.class, () -> reservation.cancel(now.plusMinutes(2)));
    }

    @Test
    void statusAt_departureBoundary_returnsCompletedWithoutChangingStoredStatus() {
        Reservation reservation = Reservation.pending(1, seat, flight, 15900);
        reservation.confirm("ABC234", LocalDateTime.of(2030, 3, 10, 12, 0));

        assertEquals(ReservationStatus.CONFIRMED, reservation.statusAt(FlightFixture.NOW));
        assertEquals(ReservationStatus.COMPLETED,
                reservation.statusAt(java.time.Clock.fixed(flight.getDepartureTime().plusSeconds(1)
                        .toInstant(java.time.ZoneOffset.UTC), java.time.ZoneOffset.UTC)));
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
    }
}