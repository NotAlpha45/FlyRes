package com.flyres.common.exception;

/** Thrown when a booking business rule is broken, for example the seat is already taken. */
public class BookingException extends RuntimeException {

    public BookingException(String message) {
        super(message);
    }
}
