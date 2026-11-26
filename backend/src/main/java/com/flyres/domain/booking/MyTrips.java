package com.flyres.domain.booking;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * A customer's reservations grouped the way the My trips screen shows them:
 * <ul>
 *   <li>{@code upcoming}: CONFIRMED and not yet departed, soonest departure first;</li>
 *   <li>{@code past}: COMPLETED (departed), most recent departure first;</li>
 *   <li>{@code cancelled}: CANCELLED, most recently cancelled first.</li>
 * </ul>
 */
public record MyTrips(List<ReservationSummary> upcoming, List<ReservationSummary> past,
                      List<ReservationSummary> cancelled) {

    private static final Comparator<ReservationSummary> BY_DEPARTURE =
            Comparator.comparing(summary -> summary.reservation().getFlight().getDepartureTime());

    public MyTrips {
        upcoming = List.copyOf(upcoming);
        past = List.copyOf(past);
        cancelled = List.copyOf(cancelled);
    }

    /** Groups and sorts {@code summaries} by their effective status. */
    public static MyTrips of(List<ReservationSummary> summaries) {
        return new MyTrips(
                summaries.stream().filter(s -> s.status() == ReservationStatus.CONFIRMED)
                        .sorted(BY_DEPARTURE).toList(),
                summaries.stream().filter(s -> s.status() == ReservationStatus.COMPLETED)
                        .sorted(BY_DEPARTURE.reversed()).toList(),
                summaries.stream().filter(s -> s.status() == ReservationStatus.CANCELLED)
                        .sorted(Comparator.comparing(MyTrips::cancelledAt).reversed()).toList());
    }

    /** True when the customer has no reservations at all. */
    public boolean isEmpty() {
        return upcoming.isEmpty() && past.isEmpty() && cancelled.isEmpty();
    }

    private static LocalDateTime cancelledAt(ReservationSummary summary) {
        return summary.reservation().getCancelledAt().orElse(LocalDateTime.MIN);
    }
}
