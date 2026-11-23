package com.flyres.control;

import com.flyres.common.exception.NotFoundException;
import com.flyres.common.validation.ValidationErrors;
import com.flyres.domain.flight.Airline;
import com.flyres.domain.flight.Airport;
import com.flyres.domain.flight.Flight;
import com.flyres.domain.flight.FlightDetails;
import com.flyres.domain.flight.FlightStatus;
import com.flyres.domain.flight.FlightSummary;
import com.flyres.domain.flight.SearchCriteria;
import com.flyres.domain.user.Role;
import com.flyres.domain.user.User;
import com.flyres.repository.AirlineRepository;
import com.flyres.repository.AirportRepository;
import com.flyres.repository.FlightRepository;
import com.flyres.repository.SeatRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * &lt;&lt;control&gt;&gt; Searching for flights and viewing a flight's details and seat map. Customers
 * and agents search; administrators manage flights elsewhere but may view any flight's details.
 * Validation errors use the field names below, which the search view registers with its
 * {@code FormValidator}.
 */
public final class FlightSearchController {

    public static final String ORIGIN = "origin";
    public static final String DESTINATION = "destination";
    public static final String DATE = "date";
    public static final String AIRLINE = "airline";

    /** Shown when a customer or agent opens a flight that is archived or has departed. */
    public static final String FLIGHT_UNAVAILABLE = "This flight is no longer available.";

    private final FlightRepository flights;
    private final SeatRepository seats;
    private final AirportRepository airports;
    private final AirlineRepository airlines;
    private final Clock clock;

    public FlightSearchController(FlightRepository flights, SeatRepository seats, AirportRepository airports,
                                  AirlineRepository airlines, Clock clock) {
        this.flights = Objects.requireNonNull(flights);
        this.seats = Objects.requireNonNull(seats);
        this.airports = Objects.requireNonNull(airports);
        this.airlines = Objects.requireNonNull(airlines);
        this.clock = Objects.requireNonNull(clock);
    }

    /** Every airport, for the origin and destination pickers. */
    public List<Airport> listAirports(User actor) {
        Access.require(actor, "search flights", Role.CUSTOMER, Role.AGENT);
        return airports.findAll();
    }

    /** Every airline, for the airline filter. */
    public List<Airline> listAirlines(User actor) {
        Access.require(actor, "search flights", Role.CUSTOMER, Role.AGENT);
        return airlines.findAll();
    }

    /**
     * Upcoming scheduled flights matching {@code criteria}, sorted by departure. Archived flights and
     * flights departing at or before now are never returned.
     */
    public List<FlightSummary> searchFlights(User actor, SearchCriteria criteria) {
        Access.require(actor, "search flights", Role.CUSTOMER, Role.AGENT);
        Objects.requireNonNull(criteria, "criteria");

        String origin = normalizeCode(criteria.originCode());
        String destination = normalizeCode(criteria.destinationCode());
        String airline = normalizeCode(criteria.airlineCode());
        LocalDate today = LocalDate.now(clock);

        ValidationErrors errors = new ValidationErrors();
        checkAirport(errors, ORIGIN, origin, "Choose where you are flying from.");
        checkAirport(errors, DESTINATION, destination, "Choose where you are flying to.");
        if (origin != null && origin.equals(destination)) {
            errors.add(DESTINATION, "Choose a destination different from the origin.");
        }
        if (criteria.date() != null && criteria.date().isBefore(today)) {
            errors.add(DATE, "Choose today or a later date.");
        }
        if (airline != null && airlines.findByCode(airline).isEmpty()) {
            errors.add(AIRLINE, "Choose an airline from the list.");
        }
        errors.throwIfAny();

        LocalDateTime from;
        LocalDateTime toExclusive;
        if (criteria.date() == null) {
            from = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
            toExclusive = null;
        } else {
            from = criteria.date().atStartOfDay();
            toExclusive = criteria.date().plusDays(1).atStartOfDay();
        }

        return flights.search(origin, destination, from, toExclusive, airline).stream()
                .filter(summary -> !summary.flight().isDeparted(clock))
                .toList();
    }

    /**
     * A flight with its seat map. Customers and agents can open only flights that are scheduled and
     * have not departed; administrators can open any flight.
     */
    public FlightDetails getFlightDetails(User actor, long flightId) {
        Access.require(actor, "view flight details", Role.CUSTOMER, Role.AGENT, Role.ADMIN);
        Flight flight = flights.findById(flightId)
                .orElseThrow(() -> new NotFoundException("This flight was not found."));
        boolean bookable = flight.getStatus() == FlightStatus.SCHEDULED && !flight.isDeparted(clock);
        if (!bookable && !actor.hasRole(Role.ADMIN)) {
            throw new NotFoundException(FLIGHT_UNAVAILABLE);
        }
        return FlightDetails.of(flight, seats.findByFlight(flightId));
    }

    private void checkAirport(ValidationErrors errors, String field, String code, String missingMessage) {
        if (code == null) {
            errors.add(field, missingMessage);
        } else if (airports.findByCode(code).isEmpty()) {
            errors.add(field, "Choose an airport from the list.");
        }
    }

    /** Trims and upper-cases an IATA code; blank becomes null. */
    private static String normalizeCode(String code) {
        return code == null || code.isBlank() ? null : code.strip().toUpperCase(Locale.ROOT);
    }
}
