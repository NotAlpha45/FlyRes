package com.flyres.common.exception;

/** Unchecked wrapper for an unexpected database failure, such as a {@link java.sql.SQLException}. */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
