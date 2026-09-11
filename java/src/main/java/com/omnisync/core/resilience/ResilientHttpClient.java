package com.omnisync.core.resilience;

import com.omnisync.core.error.NetworkException;
import com.omnisync.core.error.OmniSyncException;
import com.omnisync.core.http.HttpClient;
import com.omnisync.core.http.HttpRequest;
import com.omnisync.core.http.HttpResponse;

import java.time.Duration;
import java.util.Objects;

/**
 * Decorator wrapping any HttpClient with configurable retry, exponential backoff, and circuit breaking.
 */
public class ResilientHttpClient implements HttpClient {

    private final HttpClient underlyingClient;
    private final RetryPolicy retryPolicy;
    private final CircuitBreaker circuitBreaker;
    private final Sleeper sleeper;

    public ResilientHttpClient(HttpClient underlyingClient) {
        this(underlyingClient, RetryPolicy.defaultPolicy(), new CircuitBreaker(), Sleeper.DEFAULT);
    }

    public ResilientHttpClient(HttpClient underlyingClient, RetryPolicy retryPolicy, CircuitBreaker circuitBreaker) {
        this(underlyingClient, retryPolicy, circuitBreaker, Sleeper.DEFAULT);
    }

    public ResilientHttpClient(
            HttpClient underlyingClient,
            RetryPolicy retryPolicy,
            CircuitBreaker circuitBreaker,
            Sleeper sleeper
    ) {
        this.underlyingClient = Objects.requireNonNull(underlyingClient, "underlyingClient must not be null");
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy must not be null");
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker, "circuitBreaker must not be null");
        this.sleeper = Objects.requireNonNull(sleeper, "sleeper must not be null");
    }

    @Override
    public HttpResponse execute(HttpRequest request) throws OmniSyncException {
        int attempt = 0;

        while (true) {
            circuitBreaker.allowExecution();

            try {
                HttpResponse response = underlyingClient.execute(request);
                circuitBreaker.recordSuccess();
                return response;
            } catch (OmniSyncException error) {
                circuitBreaker.recordFailure(error);

                if (retryPolicy.shouldRetry(error, attempt)) {
                    Duration delay = retryPolicy.computeDelay(error, attempt);
                    attempt++;
                    try {
                        sleeper.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new NetworkException("Execution retry sleep interrupted", null, ie);
                    }
                } else {
                    throw error;
                }
            }
        }
    }

    public HttpClient getUnderlyingClient() {
        return underlyingClient;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }
}
