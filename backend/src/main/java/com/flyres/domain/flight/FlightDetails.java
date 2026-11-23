package com.flyres.domain.flight;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * A flight with its full seat map. The layout ({@code seatRows × seatsPerRow}) is taken from the
 * flight's own seats rather than the aircraft, because an aircraft's layout may change after the
 * flight's seats were generated.
 */
public record FlightDetails(Flight flight, int seatRows, int seatsPerRow, List<SeatAvailability> seats) {

    public FlightDetails {
        Objects.requireNonNull(flight, "flight");
        seats = List.copyOf(seats);
    }

    /** Builds the details from the flight's seats, sorted in seat-map order. */
    public static FlightDetails of(Flight flight, List<SeatAvailability> seats) {
        List<SeatAvailability> ordered = seats.stream()
                .sorted(Comparator.comparing(SeatAvailability::seat, Seat.MAP_ORDER))
                .toList();
        int rows = ordered.stream().mapToInt(s -> s.seat().row()).max().orElse(0);
        int perRow = ordered.stream().mapToInt(s -> s.seat().column() + 1).max().orElse(0);
        return new FlightDetails(flight, rows, perRow, ordered);
    }

    /** Seats with no confirmed reservation. */
    public int availableCount() {
        return (int) seats.stream().filter(s -> !s.taken()).count();
    }

    /** True when every seat is taken. */
    public boolean isSoldOut() {
        return availableCount() == 0;
    }
}
