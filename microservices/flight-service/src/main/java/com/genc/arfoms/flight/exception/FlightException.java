package com.genc.arfoms.flight.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Domain exception for flight-related errors.
 * Extends {@link ResponseStatusException} so it is handled by
 * {@link GlobalExceptionHandler} and mapped to an HTTP response.
 */
public class FlightException extends ResponseStatusException {

    /** Defaults to HTTP 400 (Bad Request) with the given message. */
    public FlightException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    /** Uses the supplied status and message. */
    public FlightException(HttpStatus status, String message) {
        super(status, message);
    }
}

