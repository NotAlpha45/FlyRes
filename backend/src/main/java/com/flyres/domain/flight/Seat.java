package com.flyres.domain.flight;

import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** One physical seat on one flight, labelled row number plus letter ({@code 1A}, {@code 12C}). */
public record Seat(long id, long flightId, String label) {

    private static final Pattern LABEL = Pattern.compile("^([1-9][0-9]?)([A-J])$");

    /** Seat-map order: by row number, then letter, so {@code 2A} comes before {@code 10A}. */
    public static final Comparator<Seat> MAP_ORDER = Comparator.comparingInt(Seat::row).thenComparing(Seat::letter);

    public Seat {
        Objects.requireNonNull(label, "label");
        if (!LABEL.matcher(label).matches()) {
            throw new IllegalArgumentException("Seat label must be a row 1-99 and a letter A-J: " + label);
        }
    }

    /** Row number, e.g. 12 for {@code 12C}. */
    public int row() {
        Matcher matcher = LABEL.matcher(label);
        matcher.matches();
        return Integer.parseInt(matcher.group(1));
    }

    /** Seat letter, e.g. 'C' for {@code 12C}. */
    public char letter() {
        return label.charAt(label.length() - 1);
    }

    /** Zero-based position across the row: A = 0, B = 1, … */
    public int column() {
        return letter() - 'A';
    }
}
