package com.flyres.ui.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class FormatsTest {

    @Test
    void money_centsToDollars() {
        assertEquals("$189.00", Formats.money(18900));
        assertEquals("$0.05", Formats.money(5));
        assertEquals("$1,234.50", Formats.money(123450));
    }

    @Test
    void date_and_dateTime() {
        assertEquals("Tue 24 Nov 2026", Formats.date(LocalDate.of(2026, 11, 24)));
        assertEquals("Tue 24 Nov, 07:05", Formats.dateTime(LocalDateTime.of(2026, 11, 24, 7, 5)));
    }

    @Test
    void arrivalTime_sameDayAndOvernight() {
        LocalDateTime departure = LocalDateTime.of(2026, 11, 24, 22, 0);

        assertEquals("23:30", Formats.arrivalTime(departure, departure.plusMinutes(90)));
        assertEquals("06:10 +1", Formats.arrivalTime(departure, LocalDateTime.of(2026, 11, 25, 6, 10)));
    }

    @Test
    void duration_hoursAndMinutes() {
        assertEquals("1h 30m", Formats.duration(Duration.ofMinutes(90)));
        assertEquals("2h", Formats.duration(Duration.ofHours(2)));
        assertEquals("50m", Formats.duration(Duration.ofMinutes(50)));
    }
}
