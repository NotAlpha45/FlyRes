package com.flyres.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flyres.common.exception.AccessDeniedException;
import com.flyres.common.exception.BookingException;
import com.flyres.common.exception.NotFoundException;
import com.flyres.data.DatabaseManager;
import com.flyres.data.TransactionRunner;
import com.flyres.data.sqlite.SqliteFlightRepository;
import com.flyres.data.sqlite.SqlitePaymentRepository;
import com.flyres.data.sqlite.SqliteReservationRepository;
import com.flyres.data.sqlite.SqliteSeatRepository;
import com.flyres.domain.booking.BookingConfirmation;
import com.flyres.domain.booking.BookingRequest;
import com.flyres.domain.booking.MyTrips;
import com.flyres.domain.booking.PaymentDetails;
import com.flyres.domain.booking.PaymentStatus;
import com.flyres.domain.booking.Reservation;
import com.flyres.domain.booking.ReservationDetails;
import com.flyres.domain.booking.ReservationStatus;
import com.flyres.domain.booking.ReservationSummary;
import com.flyres.domain.flight.Seat;
import com.flyres.domain.flight.SeatAvailability;
import com.flyres.domain.user.Role;
import com.flyres.domain.user.User;
import com.flyres.domain.user.UserFactory;
import com.flyres.fixtures.FlightFixture;
import com.flyres.repository.PaymentRepository;
import com.flyres.repository.ReservationRepository;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BookingControllerTest {

    private static final User CUSTOMER = user(Role.CUSTOMER, 1);
    private static final User OTHER_CUSTOMER = user(Role.CUSTOMER, 2);
    private static final User ADMIN = user(Role.ADMIN, 3);

    private DatabaseManager database;
    private SqliteSeatRepository seats;
    private SqlitePaymentRepository paymentRepository;
    private ReservationRepository reservations;
    private BookingController booking;

    private static User user(Role role, long id) {
        return UserFactory.create(role, id, role.name().toLowerCase() + "@example.com", "Test", role.displayName(),
                null, LocalDateTime.of(2030, 1, 1, 0, 0));
    }

    @BeforeEach
    void setUp() throws SQLException {
        database = DatabaseManager.forTesting();
        FlightFixture.insert(database.getConnection());
        SqliteFlightRepository flights = new SqliteFlightRepository(database);
        seats = new SqliteSeatRepository(database);
        paymentRepository = new SqlitePaymentRepository(database);
        reservations = new SqliteReservationRepository(database, flights);
        PaymentController payments = new PaymentController(paymentRepository, FlightFixture.NOW);
        booking = new BookingController(reservations, seats, flights, payments, new TransactionRunner(database),
                FlightFixture.NOW, new Random(12));
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    @Test
    void book_validRequest_createsConfirmedReservationAndPayment() {
        Seat seat = freeSeat(FlightFixture.WS200);

        Reservation saved = book(CUSTOMER, FlightFixture.WS200, seat, 15900, approvedCard());

        assertTrue(saved.getId() > 0);
        assertTrue(saved.getConfirmationCode().matches("[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{6}"));
        assertEquals(ReservationStatus.CONFIRMED, saved.getStatus());
        assertEquals(15900, saved.getAmountCents());
        assertEquals("WS200", saved.getFlight().getFlightNumber());
        assertTrue(seats.findByFlight(FlightFixture.WS200).stream()
                .filter(availability -> availability.seat().id() == seat.id())
                .allMatch(SeatAvailability::taken));
        var payment = paymentRepository.findByReservationId(saved.getId()).orElseThrow();
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        assertEquals("4242", payment.getCardLast4());
        assertEquals(15900, payment.getAmountCents());
    }

    @Test
    void book_returnsTheRecordedPayment() {
        Seat seat = freeSeat(FlightFixture.WS200);

        BookingConfirmation confirmation = bookWithPayment(CUSTOMER, FlightFixture.WS200, seat, 15900,
                new PaymentDetails("Maya Patel", "4242-4242-4242-4242", YearMonth.of(2030, 4), "123"));

        assertTrue(confirmation.payment().getId() > 0);
        assertEquals(confirmation.reservation().getId(), confirmation.payment().getReservationId());
        assertEquals("4242", confirmation.payment().getCardLast4());
    }

    @Test
    void book_seatAlreadyTaken_throwsBookingException() {
        Seat seat = seats.findByFlight(FlightFixture.AC104).stream()
                .filter(SeatAvailability::taken).findFirst().orElseThrow().seat();

        BookingException failure = assertThrows(BookingException.class,
                () -> book(CUSTOMER, FlightFixture.AC104, seat, 18900, approvedCard()));

        assertTrue(failure.getMessage().contains("Seat 1A was just taken"));
    }

    @Test
    void book_declinedCard_persistsNothing() throws SQLException {
        Seat seat = freeSeat(FlightFixture.WS200);
        PaymentDetails declined = new PaymentDetails("Maya Patel", "4000 0000 0000 0002",
                YearMonth.of(2030, 4), "123");

        assertThrows(BookingException.class,
                () -> book(CUSTOMER, FlightFixture.WS200, seat, 15900, declined));

        assertEquals(0, count("SELECT count(*) FROM reservations r JOIN seats s ON s.id = r.seat_id"
                + " WHERE r.customer_id = 1 AND s.flight_id = 4 AND r.status = 'CONFIRMED'"));
        assertEquals(0, count("SELECT count(*) FROM payments"));
    }

    @Test
    void book_paymentInsertFails_rollsBackReservationAndPayment() throws SQLException {
        PaymentRepository failingPayments = new PaymentRepository() {
            @Override
            public com.flyres.domain.booking.Payment insert(long reservationId,
                                                             com.flyres.domain.booking.Payment payment) {
                paymentRepository.insert(reservationId, payment);
                throw new IllegalStateException("simulated persistence failure");
            }

            @Override
            public java.util.Optional<com.flyres.domain.booking.Payment> findByReservationId(long reservationId) {
                return paymentRepository.findByReservationId(reservationId);
            }

            @Override
            public void markRefunded(long reservationId, LocalDateTime at) {
                paymentRepository.markRefunded(reservationId, at);
            }
        };
        BookingController failingBooking = new BookingController(reservations, seats,
                new SqliteFlightRepository(database), new PaymentController(failingPayments, FlightFixture.NOW),
                new TransactionRunner(database), FlightFixture.NOW, new Random(12));
        Seat seat = freeSeat(FlightFixture.WS200);

        assertThrows(IllegalStateException.class,
                () -> failingBooking.book(CUSTOMER,
                        new BookingRequest(1, FlightFixture.WS200, seat.id(), 15900, approvedCard())));

        assertEquals(0, count("SELECT count(*) FROM reservations r JOIN seats s ON s.id = r.seat_id"
                + " WHERE r.customer_id = 1 AND s.flight_id = 4 AND r.status = 'CONFIRMED'"));
        assertEquals(0, count("SELECT count(*) FROM payments"));
    }

    @Test
    void book_departedFlight_throwsBookingException() {
        assertThrows(BookingException.class,
                () -> book(CUSTOMER, 2, seats.findByFlight(2).get(0).seat(), 18900, approvedCard()));
    }

    @Test
    void book_archivedFlight_throwsBookingException() {
        assertThrows(BookingException.class,
                () -> book(CUSTOMER, FlightFixture.AC106_ARCHIVED,
                        seats.findByFlight(FlightFixture.AC106_ARCHIVED).get(0).seat(), 18900, approvedCard()));
    }

    @Test
    void book_seatFromOtherFlight_throwsBookingException() {
        Seat seat = freeSeat(FlightFixture.WS200);

        BookingException failure = assertThrows(BookingException.class,
                () -> book(CUSTOMER, FlightFixture.AC104, seat, 18900, approvedCard()));

        assertEquals(BookingController.SEAT_NOT_ON_FLIGHT, failure.getMessage());
    }

    @Test
    void book_priceChanged_throwsBookingExceptionWithCurrentFare() {
        Seat seat = freeSeat(FlightFixture.WS200);

        BookingException failure = assertThrows(BookingException.class,
                () -> book(CUSTOMER, FlightFixture.WS200, seat, 18901, approvedCard()));

        assertEquals("The fare changed to $159.00. Review and try again.", failure.getMessage());
    }

    @Test
    void book_secondSeatSameFlight_throwsBookingException() {
        Seat seat = seats.findByFlight(FlightFixture.AC104).stream()
                .filter(availability -> !availability.taken()).findFirst().orElseThrow().seat();

        BookingException failure = assertThrows(BookingException.class,
                () -> book(CUSTOMER, FlightFixture.AC104, seat, 18900, approvedCard()));

        assertEquals("You already have a seat on this flight.", failure.getMessage());
    }

    @Test
    void book_forAnotherCustomer_throwsAccessDeniedException() {
        Seat seat = freeSeat(FlightFixture.WS200);
        BookingRequest request = new BookingRequest(OTHER_CUSTOMER.getId(), FlightFixture.WS200,
                seat.id(), 15900, approvedCard());

        assertThrows(AccessDeniedException.class, () -> booking.book(CUSTOMER, request));
    }

    @Test
    void book_asAdmin_throwsAccessDeniedException() {
        Seat seat = freeSeat(FlightFixture.WS200);

        assertThrows(AccessDeniedException.class,
                () -> book(ADMIN, FlightFixture.WS200, seat, 15900, approvedCard()));
    }

    // ---------- My trips (FLY-09) ----------

    @Test
    void listMyReservations_customer_groupsOwnTripsByEffectiveStatus() throws SQLException {
        // Fixture: CONF01 on AC104 (18:00 today, upcoming) and CANC01 (cancelled), both for customer 1.
        FlightFixture.insertReservation(database.getConnection(), "PAST01", 1, "1A", "CONFIRMED");
        FlightFixture.insertReservation(database.getConnection(), "ARCH01", FlightFixture.AC106_ARCHIVED, "1A",
                "CANCELLED");
        book(CUSTOMER, FlightFixture.WS200, freeSeat(FlightFixture.WS200), 15900, approvedCard());

        MyTrips trips = booking.listMyReservations(CUSTOMER);

        assertEquals(List.of("AC104", "WS200"), flightNumbers(trips.upcoming()));
        assertEquals(List.of("AC100"), flightNumbers(trips.past()));
        assertEquals(ReservationStatus.COMPLETED, trips.past().get(0).status());
        assertEquals(2, trips.cancelled().size());
        assertTrue(trips.cancelled().stream().anyMatch(ReservationSummary::isFlightRemoved));
    }

    @Test
    void listMyReservations_returnsOnlyTheActorsReservations() {
        assertTrue(booking.listMyReservations(OTHER_CUSTOMER).isEmpty());
    }

    @Test
    void listMyReservations_asAdmin_throwsAccessDeniedException() {
        assertThrows(AccessDeniedException.class, () -> booking.listMyReservations(ADMIN));
    }

    @Test
    void getReservation_ownTrip_includesPaymentRecord() {
        Reservation saved = book(CUSTOMER, FlightFixture.WS200, freeSeat(FlightFixture.WS200), 15900, approvedCard());

        ReservationDetails details = booking.getReservation(CUSTOMER, saved.getId());

        assertEquals(saved.getConfirmationCode(), details.summary().reservation().getConfirmationCode());
        assertEquals(ReservationStatus.CONFIRMED, details.summary().status());
        assertEquals("4242", details.paymentRecord().orElseThrow().getCardLast4());
        assertEquals(PaymentStatus.COMPLETED, details.paymentRecord().orElseThrow().getStatus());
    }

    @Test
    void getReservation_otherCustomersReservation_throwsAccessDeniedException() {
        Reservation saved = book(CUSTOMER, FlightFixture.WS200, freeSeat(FlightFixture.WS200), 15900, approvedCard());

        assertThrows(AccessDeniedException.class, () -> booking.getReservation(OTHER_CUSTOMER, saved.getId()));
    }

    @Test
    void getReservation_unknownId_throwsNotFoundException() {
        NotFoundException failure = assertThrows(NotFoundException.class, () -> booking.getReservation(CUSTOMER, 999));

        assertEquals(BookingController.TRIP_NOT_FOUND, failure.getMessage());
    }

    @Test
    void getReservation_asAdmin_throwsAccessDeniedException() {
        assertThrows(AccessDeniedException.class, () -> booking.getReservation(ADMIN, 1));
    }

    // ---------- Cancel (FLY-10) ----------

    @Test
    void cancelReservation_upcoming_cancelsRefundsAndFreesTheSeat() {
        Seat seat = freeSeat(FlightFixture.WS200);
        Reservation saved = book(CUSTOMER, FlightFixture.WS200, seat, 15900, approvedCard());

        ReservationDetails cancelled = booking.cancelReservation(CUSTOMER, saved.getId());

        LocalDateTime now = LocalDateTime.now(FlightFixture.NOW);
        assertEquals(ReservationStatus.CANCELLED, cancelled.summary().status());
        assertEquals(PaymentStatus.REFUNDED, cancelled.paymentRecord().orElseThrow().getStatus());
        Reservation stored = reservations.findById(saved.getId()).orElseThrow();
        assertEquals(ReservationStatus.CANCELLED, stored.getStatus());
        assertEquals(now, stored.getCancelledAt().orElseThrow());
        var payment = paymentRepository.findByReservationId(saved.getId()).orElseThrow();
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        assertEquals(now, payment.getRefundedAt().orElseThrow());
        assertTrue(seats.findByFlight(FlightFixture.WS200).stream()
                .filter(availability -> availability.seat().id() == seat.id())
                .noneMatch(SeatAvailability::taken));
    }

    @Test
    void cancelReservation_alreadyCancelled_throwsBookingExceptionAndRefundsOnce() {
        Reservation saved = book(CUSTOMER, FlightFixture.WS200, freeSeat(FlightFixture.WS200), 15900, approvedCard());
        booking.cancelReservation(CUSTOMER, saved.getId());

        BookingException failure = assertThrows(BookingException.class,
                () -> booking.cancelReservation(CUSTOMER, saved.getId()));

        assertEquals(BookingController.ALREADY_CANCELLED, failure.getMessage());
        assertEquals(PaymentStatus.REFUNDED,
                paymentRepository.findByReservationId(saved.getId()).orElseThrow().getStatus());
    }

    @Test
    void cancelReservation_departedFlight_throwsBookingException() throws SQLException {
        // AC100 (flight 1) left at 08:00 on the fixture's "today".
        FlightFixture.insertReservation(database.getConnection(), "PAST01", 1, "1A", "CONFIRMED");
        long id = reservations.findByCustomer(1).stream()
                .filter(r -> r.getConfirmationCode().equals("PAST01")).findFirst().orElseThrow().getId();

        BookingException failure = assertThrows(BookingException.class, () -> booking.cancelReservation(CUSTOMER, id));

        assertEquals(BookingController.ALREADY_DEPARTED, failure.getMessage());
        assertEquals(ReservationStatus.CONFIRMED, reservations.findById(id).orElseThrow().getStatus());
    }

    @Test
    void cancelReservation_otherCustomer_throwsAccessDeniedException() {
        Reservation saved = book(CUSTOMER, FlightFixture.WS200, freeSeat(FlightFixture.WS200), 15900, approvedCard());

        assertThrows(AccessDeniedException.class, () -> booking.cancelReservation(OTHER_CUSTOMER, saved.getId()));
        assertEquals(ReservationStatus.CONFIRMED, reservations.findById(saved.getId()).orElseThrow().getStatus());
    }

    @Test
    void cancelReservation_unknownId_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> booking.cancelReservation(CUSTOMER, 999));
    }

    @Test
    void cancelReservation_asAdmin_throwsAccessDeniedException() {
        assertThrows(AccessDeniedException.class, () -> booking.cancelReservation(ADMIN, 1));
    }

    @Test
    void cancelReservation_refundsStoredAmountNotCurrentPrice() throws SQLException {
        Reservation saved = book(CUSTOMER, FlightFixture.WS200, freeSeat(FlightFixture.WS200), 15900, approvedCard());
        try (var statement = database.getConnection()
                .prepareStatement("UPDATE flights SET price_cents = 99900 WHERE id = ?")) {
            statement.setLong(1, FlightFixture.WS200);
            statement.executeUpdate();
        }

        ReservationDetails cancelled = booking.cancelReservation(CUSTOMER, saved.getId());

        assertEquals(15900, cancelled.paymentRecord().orElseThrow().getAmountCents());
        assertEquals(15900, paymentRepository.findByReservationId(saved.getId()).orElseThrow().getAmountCents());
    }

    @Test
    void cancelReservation_refundFails_rollsBackTheCancellation() {
        Reservation saved = book(CUSTOMER, FlightFixture.WS200, freeSeat(FlightFixture.WS200), 15900, approvedCard());
        PaymentRepository failingRefunds = new PaymentRepository() {
            @Override
            public com.flyres.domain.booking.Payment insert(long reservationId,
                                                             com.flyres.domain.booking.Payment payment) {
                return paymentRepository.insert(reservationId, payment);
            }

            @Override
            public java.util.Optional<com.flyres.domain.booking.Payment> findByReservationId(long reservationId) {
                return paymentRepository.findByReservationId(reservationId);
            }

            @Override
            public void markRefunded(long reservationId, LocalDateTime at) {
                throw new IllegalStateException("simulated refund failure");
            }
        };
        BookingController failing = new BookingController(reservations, seats, new SqliteFlightRepository(database),
                new PaymentController(failingRefunds, FlightFixture.NOW), new TransactionRunner(database),
                FlightFixture.NOW, new Random(12));

        assertThrows(IllegalStateException.class, () -> failing.cancelReservation(CUSTOMER, saved.getId()));

        assertEquals(ReservationStatus.CONFIRMED, reservations.findById(saved.getId()).orElseThrow().getStatus());
        assertEquals(PaymentStatus.COMPLETED,
                paymentRepository.findByReservationId(saved.getId()).orElseThrow().getStatus());
    }

    private static List<String> flightNumbers(List<ReservationSummary> trips) {
        return trips.stream().map(t -> t.reservation().getFlight().getFlightNumber()).toList();
    }

    @Test
    void confirmationCodeGenerator_retriesCollisionAndUsesAllowedCharacters() {
        AtomicInteger checks = new AtomicInteger();
        ReservationRepository codeRepository = new ReservationRepository() {
            @Override
            public Reservation insert(Reservation reservation) {
                throw new UnsupportedOperationException();
            }

            @Override
            public java.util.Optional<Reservation> findById(long id) {
                return java.util.Optional.empty();
            }

            @Override
            public java.util.List<Reservation> findByCustomer(long customerId) {
                return java.util.List.of();
            }

            @Override
            public void markCancelled(long reservationId, LocalDateTime at) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean existsByCode(String confirmationCode) {
                return checks.getAndIncrement() == 0;
            }

            @Override
            public boolean hasConfirmedOnFlight(long customerId, long flightId) {
                return false;
            }
        };
        ConfirmationCodeGenerator generator = new ConfirmationCodeGenerator(new Random(5));

        String code = generator.next(codeRepository);

        assertEquals(2, checks.get());
        assertTrue(code.matches("[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{6}"));
    }

    @Test
    void confirmationCodeGenerator_sameSeed_returnsDeterministicCode() {
        String first = new ConfirmationCodeGenerator(new Random(42)).next(emptyCodeRepository());
        String second = new ConfirmationCodeGenerator(new Random(42)).next(emptyCodeRepository());

        assertEquals(first, second);
    }

    private static ReservationRepository emptyCodeRepository() {
        return new ReservationRepository() {
            @Override
            public Reservation insert(Reservation reservation) {
                throw new UnsupportedOperationException();
            }

            @Override
            public java.util.Optional<Reservation> findById(long id) {
                return java.util.Optional.empty();
            }

            @Override
            public java.util.List<Reservation> findByCustomer(long customerId) {
                return java.util.List.of();
            }

            @Override
            public void markCancelled(long reservationId, LocalDateTime at) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean existsByCode(String confirmationCode) {
                return false;
            }

            @Override
            public boolean hasConfirmedOnFlight(long customerId, long flightId) {
                return false;
            }
        };
    }

    private Seat freeSeat(long flightId) {
        return seats.findByFlight(flightId).stream()
                .filter(availability -> !availability.taken())
                .map(SeatAvailability::seat)
                .findFirst().orElseThrow();
    }

    private Reservation book(User actor, long flightId, Seat seat, long expectedPrice, PaymentDetails payment) {
        return bookWithPayment(actor, flightId, seat, expectedPrice, payment).reservation();
    }

    private BookingConfirmation bookWithPayment(User actor, long flightId, Seat seat, long expectedPrice,
                                                PaymentDetails payment) {
        return booking.book(actor, new BookingRequest(actor.getId(), flightId, seat.id(), expectedPrice, payment));
    }

    private static PaymentDetails approvedCard() {
        return new PaymentDetails("Maya Patel", "4242 4242 4242 4242", YearMonth.of(2030, 4), "123");
    }

    private int count(String sql) throws SQLException {
        try (var statement = database.getConnection().createStatement();
             var rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getInt(1);
        }
    }
}