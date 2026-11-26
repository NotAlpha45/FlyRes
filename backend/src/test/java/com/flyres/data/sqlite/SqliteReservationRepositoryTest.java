package com.flyres.data.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flyres.common.exception.BookingException;
import com.flyres.common.exception.DataAccessException;
import com.flyres.data.DatabaseManager;
import com.flyres.domain.booking.Reservation;
import com.flyres.domain.booking.ReservationStatus;
import com.flyres.domain.flight.Flight;
import com.flyres.domain.flight.Seat;
import com.flyres.fixtures.FlightFixture;
import java.sql.SQLException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqliteReservationRepositoryTest {

    private DatabaseManager database;
    private SqliteFlightRepository flights;
    private SqliteSeatRepository seats;
    private SqliteReservationRepository reservations;

    @BeforeEach
    void setUp() throws SQLException {
        database = DatabaseManager.forTesting();
        FlightFixture.insert(database.getConnection());
        flights = new SqliteFlightRepository(database);
        seats = new SqliteSeatRepository(database);
        reservations = new SqliteReservationRepository(database, flights);
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    @Test
    void insert_thenFind_restoresReservationAndFlight() {
        Reservation saved = reservations.insert(confirmed(FlightFixture.WS200, "QW2345", "1A"));
        Reservation found = reservations.findById(saved.getId()).orElseThrow();

        assertTrue(saved.getId() > 0);
        assertEquals("QW2345", found.getConfirmationCode());
        assertEquals(1, found.getCustomerId());
        assertEquals("1A", found.getSeat().label());
        assertEquals("WS200", found.getFlight().getFlightNumber());
        assertEquals(15900, found.getAmountCents());
        assertEquals(ReservationStatus.CONFIRMED, found.getStatus());
        assertEquals(LocalDateTime.of(2030, 3, 10, 12, 0), found.getCreatedAt().orElseThrow());
        assertTrue(reservations.existsByCode("QW2345"));
        assertTrue(reservations.hasConfirmedOnFlight(1, FlightFixture.WS200));
    }

    @Test
    void insert_sameConfirmedSeat_translatesUniqueIndexViolation() {
        Reservation first = confirmed(FlightFixture.WS200, "QW2345", "1A");
        Reservation second = confirmed(FlightFixture.WS200, "RT6789", "1A");
        reservations.insert(first);

        BookingException failure = assertThrows(BookingException.class, () -> reservations.insert(second));

        assertEquals("Seat 1A was just taken. Pick another seat.", failure.getMessage());
    }

    @Test
    void insert_duplicateConfirmationCode_isNotReportedAsSeatTaken() {
        reservations.insert(confirmed(FlightFixture.WS200, "QW2345", "1A"));

        assertThrows(DataAccessException.class,
                () -> reservations.insert(confirmed(FlightFixture.WS200, "QW2345", "1B")));
    }

    @Test
    void findById_unknownId_isEmpty() {
        assertFalse(reservations.findById(999).isPresent());
    }

    private Reservation confirmed(long flightId, String code, String label) {
        Flight flight = flights.findById(flightId).orElseThrow();
        Seat seat = seats.findByFlight(flightId).stream().map(availability -> availability.seat())
                .filter(candidate -> candidate.label().equals(label)).findFirst().orElseThrow();
        Reservation reservation = Reservation.pending(1, seat, flight, flight.getPriceCents());
        reservation.confirm(code, LocalDateTime.of(2030, 3, 10, 12, 0));
        return reservation;
    }
}