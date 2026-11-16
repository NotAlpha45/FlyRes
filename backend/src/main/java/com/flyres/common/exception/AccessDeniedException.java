package com.flyres.common.exception;

/** Thrown by a control when the acting user's role may not perform the requested operation. */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
