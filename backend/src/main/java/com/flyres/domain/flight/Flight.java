package com.flyres.domain.flight;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A scheduled departure of one aircraft on one route. Times are local airport time with no
 * time-zone conversion (a documented simplification). The price is in cents and applies to every
 * seat; existing reservations keep the amount they paid.
 */
public final class Flight {

    private final long id;
    private final String flightNumber;
    private final Route route;
    private final Aircraft aircraft;
    private final LocalDateTime departureTime;
    private final LocalDateTime arrivalTime;
    private final long priceCents;
    private final FlightStatus status;

    public Flight(long id, String flightNumber, Route route, Aircraft aircraft, LocalDateTime departureTime,
                  LocalDateTime arrivalTime, long priceCents, FlightStatus status) {
        this.id = id;
        this.flightNumber = Objects.requireNonNull(flightNumber, "flightNumber");
        this.route = Objects.requireNonNull(route, "route");
        this.aircraft = Objects.requireNonNull(aircraft, "aircraft");
        this.departureTime = Objects.requireNonNull(departureTime, "departureTime");
        this.arrivalTime = Objects.requireNonNull(arrivalTime, "arrivalTime");
        this.priceCents = priceCents;
        this.status = Objects.requireNonNull(status, "status");
        if (!arrivalTime.isAfter(departureTime)) {
            throw new IllegalArgumentException("Arrival must be after departure");
        }
        if (priceCents < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
    }

    /** True once the departure time is at or before "now" on {@code clock}. */
    public boolean isDeparted(Clock clock) {
        return !departureTime.isAfter(LocalDateTime.now(clock));
    }

    /** Scheduled time in the air. */
    public Duration duration() {
        return Duration.between(departureTime, arrivalTime);
    }

    /** The operating airline, which is the aircraft's airline. */
    public Airline airline() {
        return aircraft.airline();
    }

    public long getId() {
        return id;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public Route getRoute() {
        return route;
    }

    public Aircraft getAircraft() {
        return aircraft;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public long getPriceCents() {
        return priceCents;
    }

    public FlightStatus getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Flight flight && flight.id == id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return "Flight[id=" + id + ", " + flightNumber + " " + route.origin().code() + "->"
                + route.destination().code() + " " + departureTime + ", " + status + "]";
    }
}
