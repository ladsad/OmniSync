package com.omnisync.core.error;

import java.time.Duration;
import java.util.Optional;

/**
 * Exception thrown when requests are short-circuited because a circuit breaker is in OPEN state.
 */
public class CircuitBreakerOpenException extends OmniSyncException {

    private final Duration remainingTimeout;

    /**
     * Constructs a CircuitBreakerOpenException with message and remaining lockout duration.
     *
     * @param message descriptive message
     * @param remainingTimeout estimated duration until circuit breaker tries half-open
     */
    public CircuitBreakerOpenException(String message, Duration remainingTimeout) {
        super(message);
        this.remainingTimeout = remainingTimeout;
    }

    public CircuitBreakerOpenException(String message) {
        this(message, null);
    }

    /**
     * Returns remaining lockout duration before half-open probe is permitted.
     *
     * @return optional containing remaining duration
     */
    public Optional<Duration> getRemainingTimeout() {
        return Optional.ofNullable(remainingTimeout);
    }
}
