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

    // ---------- changeSeat (FLY-11) ----------

    private Reservation confirmed() {
        Reservation reservation = Reservation.pending(1, seat, flight, 15900);
        reservation.confirm("ABC234", LocalDateTime.of(2030, 3, 10, 12, 0));
        return reservation;
    }

    private Seat otherSeatOn(long flightId) throws SQLException {
        return new SqliteSeatRepository(database).findByFlight(flightId).stream()
                .map(availability -> availability.seat())
                .filter(candidate -> candidate.id() != seat.id())
                .findFirst().orElseThrow();
    }

    @Test
    void changeSeat_confirmed_movesSeatAndKeepsEverythingElse() throws SQLException {
        Reservation reservation = confirmed();
        Seat newSeat = otherSeatOn(FlightFixture.WS200);

        reservation.changeSeat(newSeat);

        assertEquals(newSeat, reservation.getSeat());
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals("ABC234", reservation.getConfirmationCode());
        assertEquals(15900, reservation.getAmountCents());
    }

    @Test
    void changeSeat_cancelledOrPending_throwsIllegalState() throws SQLException {
        Seat newSeat = otherSeatOn(FlightFixture.WS200);
        Reservation cancelled = confirmed();
        cancelled.cancel(LocalDateTime.of(2030, 3, 10, 13, 0));

        assertThrows(IllegalStateException.class, () -> cancelled.changeSeat(newSeat));
        assertThrows(IllegalStateException.class,
                () -> Reservation.pending(1, seat, flight, 15900).changeSeat(newSeat));
    }

    @Test
    void changeSeat_seatOnAnotherFlight_throwsIllegalArgument() throws SQLException {
        Reservation reservation = confirmed();
        Seat otherFlightSeat = otherSeatOn(FlightFixture.AC104);

        assertThrows(IllegalArgumentException.class, () -> reservation.changeSeat(otherFlightSeat));
    }

    @Test
    void changeSeat_sameSeat_throwsIllegalArgument() {
        Reservation reservation = confirmed();

        assertThrows(IllegalArgumentException.class, () -> reservation.changeSeat(seat));
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