package com.omnisync.core.error;

import java.time.Duration;
import java.util.Optional;

/**
 * Exception thrown when a SaaS provider rate limit is encountered (e.g. HTTP 429).
 */
public class RateLimitExceededException extends OmniSyncException {

    private final Duration retryAfter;

    /**
     * Constructs a new RateLimitExceededException without a retry duration.
     *
     * @param message the detail message
     */
    public RateLimitExceededException(String message) {
        this(message, (Duration) null);
    }

    /**
     * Constructs a new RateLimitExceededException with an explicit retry duration.
     *
     * @param message the detail message
     * @param retryAfter expected wait duration before retry, or null if unknown
     */
    public RateLimitExceededException(String message, Duration retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    /**
     * Constructs a new RateLimitExceededException with an explicit retry duration and cause.
     *
     * @param message the detail message
     * @param retryAfter expected wait duration before retry, or null if unknown
     * @param cause the underlying cause
     */
    public RateLimitExceededException(String message, Duration retryAfter, Throwable cause) {
        super(message, cause);
        this.retryAfter = retryAfter;
    }

    /**
     * Returns the recommended retry delay if provided by the provider headers.
     *
     * @return optional containing the duration to wait before retrying
     */
    public Optional<Duration> getRetryAfter() {
        return Optional.ofNullable(retryAfter);
    }
}
