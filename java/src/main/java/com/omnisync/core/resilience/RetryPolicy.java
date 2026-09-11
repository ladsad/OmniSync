package com.omnisync.core.resilience;

import com.omnisync.core.error.NetworkException;
import com.omnisync.core.error.RateLimitExceededException;

import java.time.Duration;
import java.util.Objects;

/**
 * Strategy defining retry conditions and exponential backoff calculations for transient errors.
 */
public class RetryPolicy {

    public static final int DEFAULT_MAX_RETRIES = 3;
    public static final Duration DEFAULT_INITIAL_BACKOFF = Duration.ofMillis(500);
    public static final Duration DEFAULT_MAX_BACKOFF = Duration.ofSeconds(30);
    public static final double DEFAULT_MULTIPLIER = 2.0;

    private final int maxRetries;
    private final Duration initialBackoff;
    private final Duration maxBackoff;
    private final double backoffMultiplier;
    private final boolean respectRetryAfter;

    public RetryPolicy(
            int maxRetries,
            Duration initialBackoff,
            Duration maxBackoff,
            double backoffMultiplier,
            boolean respectRetryAfter
    ) {
        this.maxRetries = Math.max(0, maxRetries);
        this.initialBackoff = Objects.requireNonNull(initialBackoff, "initialBackoff must not be null");
        this.maxBackoff = Objects.requireNonNull(maxBackoff, "maxBackoff must not be null");
        this.backoffMultiplier = backoffMultiplier >= 1.0 ? backoffMultiplier : 1.0;
        this.respectRetryAfter = respectRetryAfter;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static RetryPolicy defaultPolicy() {
        return builder().build();
    }

    /**
     * Determines whether the given failure should be retried at the specified attempt index.
     *
     * @param error caught exception
     * @param attempt zero-based attempt counter (0 is first execution failure)
     * @return true if error is transient and attempt < maxRetries
     */
    public boolean shouldRetry(Throwable error, int attempt) {
        if (attempt >= maxRetries) {
            return false;
        }
        return (error instanceof RateLimitExceededException) || (error instanceof NetworkException);
    }

    /**
     * Computes wait duration before executing the next retry attempt.
     *
     * @param error caught exception
     * @param attempt zero-based attempt counter
     * @return delay duration
     */
    public Duration computeDelay(Throwable error, int attempt) {
        if (respectRetryAfter && error instanceof RateLimitExceededException rle) {
            if (rle.getRetryAfter().isPresent()) {
                Duration retryAfter = rle.getRetryAfter().get();
                return retryAfter.compareTo(maxBackoff) > 0 ? maxBackoff : retryAfter;
            }
        }

        double factor = Math.pow(backoffMultiplier, attempt);
        long delayMillis = (long) (initialBackoff.toMillis() * factor);
        Duration calculated = Duration.ofMillis(delayMillis);

        return calculated.compareTo(maxBackoff) > 0 ? maxBackoff : calculated;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public Duration getInitialBackoff() {
        return initialBackoff;
    }

    public Duration getMaxBackoff() {
        return maxBackoff;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public boolean isRespectRetryAfter() {
        return respectRetryAfter;
    }

    public static class Builder {
        private int maxRetries = DEFAULT_MAX_RETRIES;
        private Duration initialBackoff = DEFAULT_INITIAL_BACKOFF;
        private Duration maxBackoff = DEFAULT_MAX_BACKOFF;
        private double backoffMultiplier = DEFAULT_MULTIPLIER;
        private boolean respectRetryAfter = true;

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder initialBackoff(Duration initialBackoff) {
            this.initialBackoff = initialBackoff;
            return this;
        }

        public Builder maxBackoff(Duration maxBackoff) {
            this.maxBackoff = maxBackoff;
            return this;
        }

        public Builder backoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
            return this;
        }

        public Builder respectRetryAfter(boolean respectRetryAfter) {
            this.respectRetryAfter = respectRetryAfter;
            return this;
        }

        public RetryPolicy build() {
            return new RetryPolicy(maxRetries, initialBackoff, maxBackoff, backoffMultiplier, respectRetryAfter);
        }
    }
}
