package com.omnisync.core.error;

/**
 * Exception thrown when authentication or authorization fails against a SaaS API.
 */
public class AuthenticationException extends OmniSyncException {

    /**
     * Constructs a new AuthenticationException with the specified message.
     *
     * @param message the detail message
     */
    public AuthenticationException(String message) {
        super(message);
    }

    /**
     * Constructs a new AuthenticationException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the underlying cause
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
