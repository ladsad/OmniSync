package com.omnisync.core.error;

/**
 * Base unchecked exception for all OmniSync framework errors.
 */
public class OmniSyncException extends RuntimeException {

    /**
     * Constructs a new OmniSyncException with the specified message.
     *
     * @param message the detail message
     */
    public OmniSyncException(String message) {
        super(message);
    }

    /**
     * Constructs a new OmniSyncException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the underlying cause
     */
    public OmniSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
