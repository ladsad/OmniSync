package com.omnisync.core.error;

import java.util.OptionalInt;

/**
 * Exception thrown when network-level communication fails (connection failure, timeout, 5xx).
 */
public class NetworkException extends OmniSyncException {

    private final Integer statusCode;

    /**
     * Constructs a new NetworkException without an HTTP status code.
     *
     * @param message the detail message
     */
    public NetworkException(String message) {
        this(message, (Integer) null);
    }

    /**
     * Constructs a new NetworkException with an HTTP status code.
     *
     * @param message the detail message
     * @param statusCode the HTTP response status code, or null if transport-level failure
     */
    public NetworkException(String message, Integer statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Constructs a new NetworkException with an HTTP status code and cause.
     *
     * @param message the detail message
     * @param statusCode the HTTP response status code, or null if transport-level failure
     * @param cause the underlying cause
     */
    public NetworkException(String message, Integer statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /**
     * Returns the HTTP status code associated with the network failure, if any.
     *
     * @return optional integer status code
     */
    public OptionalInt getStatusCode() {
        return statusCode != null ? OptionalInt.of(statusCode) : OptionalInt.empty();
    }
}
