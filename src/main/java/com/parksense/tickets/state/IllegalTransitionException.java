package com.parksense.tickets.state;

/**
 * Thrown when an operation is attempted from a ticket state that does not
 * allow it (paying an exited ticket, exiting twice…). Maps to HTTP 409 at
 * the API edge.
 */
public class IllegalTransitionException extends RuntimeException {

    public IllegalTransitionException(String message) {
        super(message);
    }
}
