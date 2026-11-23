package com.flyres.ui.component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Display formatting for the UI. The backend keeps money as {@code long} cents and times as
 * {@code java.time} values; they are turned into text only here.
 */
public final class Formats {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    private Formats() {
    }

    /** {@code 18900} → {@code "$189.00"} (Canadian dollars). */
    public static String money(long cents) {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.CANADA);
        return format.format(BigDecimal.valueOf(cents, 2));
    }

    /** {@code "Mon 24 Nov 2026"}. */
    public static String date(LocalDate date) {
        return DATE.format(date);
    }

    /** {@code "Mon 24 Nov, 07:00"}. */
    public static String dateTime(LocalDateTime dateTime) {
        return DATE_TIME.format(dateTime);
    }

    /**
     * Arrival time relative to departure: {@code "08:30"} on the same day, {@code "06:10 +1"} the next
     * day, {@code "06:10 +2"} two days later.
     */
    public static String arrivalTime(LocalDateTime departure, LocalDateTime arrival) {
        long days = arrival.toLocalDate().toEpochDay() - departure.toLocalDate().toEpochDay();
        String time = TIME.format(arrival);
        return days > 0 ? time + " +" + days : time;
    }

    /** {@code "1h 30m"}, {@code "2h"} or {@code "45m"}. */
    public static String duration(Duration duration) {
        long hours = duration.toHours();
        int minutes = duration.toMinutesPart();
        if (hours == 0) {
            return minutes + "m";
        }
        return minutes == 0 ? hours + "h" : hours + "h " + minutes + "m";
    }
}
