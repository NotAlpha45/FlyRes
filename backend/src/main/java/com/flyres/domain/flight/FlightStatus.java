package com.flyres.domain.flight;

/**
 * Stored status of a flight. Flights are never deleted: removing an outdated flight archives it.
 * "Departed" is not a status; it is derived from the departure time.
 */
public enum FlightStatus {
    SCHEDULED,
    ARCHIVED
}
