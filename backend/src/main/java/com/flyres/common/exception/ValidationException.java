package com.flyres.common.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thrown by a control when its input is invalid. Carries one message per invalid field, keyed by
 * a field name the view knows, so a form can show every error inline under the right field at once.
 */
public class ValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    /** A single invalid field. */
    public ValidationException(String field, String message) {
        this(Map.of(field, message));
    }

    /** Several invalid fields, in the order the form shows them. */
    public ValidationException(Map<String, String> fieldErrors) {
        super(firstMessage(fieldErrors));
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    /** The first invalid field. */
    public String getField() {
        return fieldErrors.keySet().iterator().next();
    }

    /** Every invalid field mapped to its message, in form order. */
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    private static String firstMessage(Map<String, String> fieldErrors) {
        if (fieldErrors.isEmpty()) {
            throw new IllegalArgumentException("A ValidationException needs at least one field error");
        }
        return fieldErrors.values().iterator().next();
    }
}
