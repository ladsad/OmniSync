package com.omnisync.core.error;

import java.util.Optional;

/**
 * Exception thrown when payload data from a SaaS provider cannot be parsed or validated.
 */
public class MalformedDataException extends OmniSyncException {

    private final String rawPayload;

    /**
     * Constructs a new MalformedDataException without raw payload context.
     *
     * @param message the detail message
     */
    public MalformedDataException(String message) {
        this(message, (String) null);
    }

    /**
     * Constructs a new MalformedDataException with raw payload context.
     *
     * @param message the detail message
     * @param rawPayload the unparsed or invalid raw payload snippet
     */
    public MalformedDataException(String message, String rawPayload) {
        super(message);
        this.rawPayload = rawPayload;
    }

    /**
     * Constructs a new MalformedDataException with raw payload context and cause.
     *
     * @param message the detail message
     * @param rawPayload the unparsed or invalid raw payload snippet
     * @param cause the underlying cause
     */
    public MalformedDataException(String message, String rawPayload, Throwable cause) {
        super(message, cause);
        this.rawPayload = rawPayload;
    }

    /**
     * Returns the raw payload snippet associated with this failure, if available.
     *
     * @return optional containing the raw payload
     */
    public Optional<String> getRawPayload() {
        return Optional.ofNullable(rawPayload);
    }
}
