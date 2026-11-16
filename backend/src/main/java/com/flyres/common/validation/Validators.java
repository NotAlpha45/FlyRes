package com.flyres.common.validation;

import java.util.Locale;
import java.util.regex.Pattern;

/** Reusable input checks. Controls combine them and report failures through {@link ValidationErrors}. */
public final class Validators {

    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 128;
    public static final int NAME_MAX_LENGTH = 50;
    public static final int EMAIL_MAX_LENGTH = 254;

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9 ()\\-]{7,20}$");
    private static final Pattern LETTER = Pattern.compile("\\p{L}");
    private static final Pattern DIGIT = Pattern.compile("\\d");

    private Validators() {
    }

    /** True if {@code value} is null, empty or only whitespace. */
    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** True if {@code value} looks like {@code name@domain.tld} with no spaces, ignoring surrounding whitespace. */
    public static boolean isEmail(String value) {
        if (value == null) {
            return false;
        }
        String email = value.strip();
        return email.length() <= EMAIL_MAX_LENGTH && EMAIL.matcher(email).matches();
    }

    /** True if {@code value} has 8–128 characters with at least one letter and one digit. */
    public static boolean isStrongPassword(String value) {
        return value != null
                && value.length() >= PASSWORD_MIN_LENGTH
                && value.length() <= PASSWORD_MAX_LENGTH
                && LETTER.matcher(value).find()
                && DIGIT.matcher(value).find();
    }

    /** True if {@code value} is 7–20 digits, spaces, dashes or parentheses, with an optional leading +. */
    public static boolean isPhone(String value) {
        return value != null && PHONE.matcher(value.strip()).matches();
    }

    /** Trims and lower-cases an email so lookups and storage agree. */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    /** Trims {@code value}, turning a blank value into null. */
    public static String trimToNull(String value) {
        return isBlank(value) ? null : value.strip();
    }
}
