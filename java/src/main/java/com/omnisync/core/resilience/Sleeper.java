package com.omnisync.core.resilience;

import java.time.Duration;

/**
 * Functional interface abstracting thread sleep for deterministic testing of backoff delays.
 */
@FunctionalInterface
public interface Sleeper {

    /**
     * Pauses current thread execution for the specified duration.
     *
     * @param duration duration to sleep
     * @throws InterruptedException if interrupted during sleep
     */
    void sleep(Duration duration) throws InterruptedException;

    /**
     * Default sleeper invoking Thread.sleep().
     */
    Sleeper DEFAULT = duration -> {
        if (duration != null && !duration.isNegative() && !duration.isZero()) {
            Thread.sleep(duration.toMillis());
        }
    };
}
