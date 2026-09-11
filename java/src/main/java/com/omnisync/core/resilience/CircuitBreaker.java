package com.omnisync.core.resilience;

import com.omnisync.core.error.CircuitBreakerOpenException;
import com.omnisync.core.error.NetworkException;
import com.omnisync.core.error.RateLimitExceededException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * State machine protecting upstream services from cascading failures by short-circuiting requests.
 */
public class CircuitBreaker {

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    public static final int DEFAULT_FAILURE_THRESHOLD = 5;
    public static final Duration DEFAULT_RECOVERY_TIMEOUT = Duration.ofSeconds(30);
    public static final int DEFAULT_SUCCESS_THRESHOLD = 2;

    private final int failureThreshold;
    private final Duration recoveryTimeout;
    private final int successThreshold;

    private State state = State.CLOSED;
    private int failureCount = 0;
    private int halfOpenSuccessCount = 0;
    private Instant lastFailureTime;

    public CircuitBreaker() {
        this(DEFAULT_FAILURE_THRESHOLD, DEFAULT_RECOVERY_TIMEOUT, DEFAULT_SUCCESS_THRESHOLD);
    }

    public CircuitBreaker(int failureThreshold, Duration recoveryTimeout, int successThreshold) {
        this.failureThreshold = Math.max(1, failureThreshold);
        this.recoveryTimeout = Objects.requireNonNull(recoveryTimeout, "recoveryTimeout must not be null");
        this.successThreshold = Math.max(1, successThreshold);
    }

    /**
     * Evaluates circuit state, allowing execution or throwing CircuitBreakerOpenException.
     *
     * @throws CircuitBreakerOpenException if circuit is in OPEN state
     */
    public synchronized void allowExecution() throws CircuitBreakerOpenException {
        Instant now = Instant.now();

        if (state == State.OPEN) {
            Duration elapsed = Duration.between(lastFailureTime, now);
            if (elapsed.compareTo(recoveryTimeout) >= 0) {
                state = State.HALF_OPEN;
                halfOpenSuccessCount = 0;
            } else {
                Duration remaining = recoveryTimeout.minus(elapsed);
                throw new CircuitBreakerOpenException(
                        "Circuit breaker is OPEN. Requests short-circuited.",
                        remaining
                );
            }
        }
    }

    /**
     * Records a successful operation, potentially closing the circuit if in HALF_OPEN.
     */
    public synchronized void recordSuccess() {
        if (state == State.HALF_OPEN) {
            halfOpenSuccessCount++;
            if (halfOpenSuccessCount >= successThreshold) {
                state = State.CLOSED;
                failureCount = 0;
                halfOpenSuccessCount = 0;
            }
        } else if (state == State.CLOSED) {
            failureCount = 0;
        }
    }

    /**
     * Records an execution failure, opening the circuit if thresholds are exceeded.
     *
     * @param error the caught exception
     */
    public synchronized void recordFailure(Throwable error) {
        if (!(error instanceof NetworkException || error instanceof RateLimitExceededException)) {
            return;
        }

        Instant now = Instant.now();
        lastFailureTime = now;

        if (state == State.HALF_OPEN) {
            state = State.OPEN;
            failureCount = failureThreshold;
            halfOpenSuccessCount = 0;
        } else if (state == State.CLOSED) {
            failureCount++;
            if (failureCount >= failureThreshold) {
                state = State.OPEN;
            }
        }
    }

    public synchronized void reset() {
        state = State.CLOSED;
        failureCount = 0;
        halfOpenSuccessCount = 0;
        lastFailureTime = null;
    }

    public synchronized State getState() {
        return state;
    }

    public synchronized int getFailureCount() {
        return failureCount;
    }

    public Duration getRecoveryTimeout() {
        return recoveryTimeout;
    }
}
