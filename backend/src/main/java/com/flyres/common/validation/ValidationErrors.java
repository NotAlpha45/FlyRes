package com.flyres.common.validation;

import com.flyres.common.exception.ValidationException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Collects every invalid field of one request so the user sees all errors at once,
 * then throws a single {@link ValidationException}. The first error recorded for a field wins.
 */
public final class ValidationErrors {

    private final Map<String, String> errors = new LinkedHashMap<>();

    /** Records {@code message} for {@code field} unless the field already has an error. */
    public ValidationErrors add(String field, String message) {
        errors.putIfAbsent(field, message);
        return this;
    }

    /** True if {@code field} already has an error. */
    public boolean has(String field) {
        return errors.containsKey(field);
    }

    /** Throws a {@link ValidationException} with every recorded error, if there are any. */
    public void throwIfAny() {
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
