package com.flyres.common.exception;

/** Thrown when a requested record, such as a flight or reservation, does not exist. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
