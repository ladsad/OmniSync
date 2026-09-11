package com.omnisync.core.resilience;

import com.omnisync.core.error.AuthenticationException;
import com.omnisync.core.error.CircuitBreakerOpenException;
import com.omnisync.core.error.MalformedDataException;
import com.omnisync.core.error.NetworkException;
import com.omnisync.core.error.RateLimitExceededException;
import com.omnisync.core.http.HttpClient;
import com.omnisync.core.http.HttpRequest;
import com.omnisync.core.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResilienceTest {

    @Test
    @DisplayName("RetryPolicy classifies retryable vs non-retryable exceptions")
    void retryPolicyClassification() {
        RetryPolicy policy = RetryPolicy.builder().maxRetries(2).build();

        assertThat(policy.shouldRetry(new NetworkException("timeout", 503), 0)).isTrue();
        assertThat(policy.shouldRetry(new RateLimitExceededException("rate limit"), 1)).isTrue();

        // Attempts exhausted
        assertThat(policy.shouldRetry(new NetworkException("timeout", 503), 2)).isFalse();

        // Non-transient errors
        assertThat(policy.shouldRetry(new AuthenticationException("unauthorized"), 0)).isFalse();
        assertThat(policy.shouldRetry(new MalformedDataException("corrupt"), 0)).isFalse();
    }

    @Test
    @DisplayName("RetryPolicy calculates exponential backoff and respects Retry-After")
    void retryPolicyDelayCalculations() {
        RetryPolicy policy = RetryPolicy.builder()
                .initialBackoff(Duration.ofMillis(100))
                .backoffMultiplier(2.0)
                .maxBackoff(Duration.ofMillis(500))
                .respectRetryAfter(true)
                .build();

        assertThat(policy.computeDelay(new NetworkException("error"), 0)).isEqualTo(Duration.ofMillis(100));
        assertThat(policy.computeDelay(new NetworkException("error"), 1)).isEqualTo(Duration.ofMillis(200));
        assertThat(policy.computeDelay(new NetworkException("error"), 2)).isEqualTo(Duration.ofMillis(400));
        assertThat(policy.computeDelay(new NetworkException("error"), 3)).isEqualTo(Duration.ofMillis(500)); // Capped

        RateLimitExceededException rle = new RateLimitExceededException("rate limit", Duration.ofMillis(250));
        assertThat(policy.computeDelay(rle, 0)).isEqualTo(Duration.ofMillis(250));
    }

    @Test
    @DisplayName("CircuitBreaker transitions across CLOSED, OPEN, HALF_OPEN, and CLOSED states")
    void circuitBreakerStateTransitions() {
        CircuitBreaker breaker = new CircuitBreaker(2, Duration.ofMillis(50), 1);
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Non-transient error does not increment failure count
        breaker.recordFailure(new AuthenticationException("auth error"));
        assertThat(breaker.getFailureCount()).isEqualTo(0);

        // First network failure
        breaker.recordFailure(new NetworkException("fail 1", 500));
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Second failure triggers OPEN
        breaker.recordFailure(new NetworkException("fail 2", 500));
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // In OPEN state, fast-fails immediately
        assertThatThrownBy(breaker::allowExecution)
                .isInstanceOf(CircuitBreakerOpenException.class);

        // Wait past recovery timeout
        try {
            Thread.sleep(60);
        } catch (InterruptedException ignored) {}

        // allowExecution transitions to HALF_OPEN
        breaker.allowExecution();
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        // Success closes the circuit
        breaker.recordSuccess();
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(breaker.getFailureCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("ResilientHttpClient retries transient errors and succeeds")
    void resilientHttpClientRetriesAndSucceeds() {
        AtomicInteger attempts = new AtomicInteger(0);
        HttpClient mockClient = request -> {
            int count = attempts.incrementAndGet();
            if (count < 3) {
                throw new NetworkException("temporary error", 503);
            }
            return new HttpResponse(200, Map.of(), "{\"ok\":true}");
        };

        List<Duration> sleptDurations = new ArrayList<>();
        Sleeper mockSleeper = sleptDurations::add;

        RetryPolicy retryPolicy = RetryPolicy.builder().maxRetries(3).initialBackoff(Duration.ofMillis(10)).build();
        CircuitBreaker breaker = new CircuitBreaker();

        ResilientHttpClient client = new ResilientHttpClient(mockClient, retryPolicy, breaker, mockSleeper);
        HttpResponse response = client.execute(HttpRequest.get("https://api.example.com/data"));

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(attempts.get()).isEqualTo(3);
        assertThat(sleptDurations).hasSize(2);
    }

    @Test
    @DisplayName("ResilientHttpClient does not retry non-transient errors")
    void resilientHttpClientFailsImmediatelyOnAuthError() {
        AtomicInteger attempts = new AtomicInteger(0);
        HttpClient mockClient = request -> {
            attempts.incrementAndGet();
            throw new AuthenticationException("invalid token");
        };

        ResilientHttpClient client = new ResilientHttpClient(mockClient);
        assertThatThrownBy(() -> client.execute(HttpRequest.get("https://api.example.com/data")))
                .isInstanceOf(AuthenticationException.class);

        assertThat(attempts.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("ResilientHttpClient short-circuits via CircuitBreaker when failures threshold is exceeded")
    void resilientHttpClientCircuitBreakerTripping() {
        HttpClient mockClient = request -> {
            throw new NetworkException("downstream offline", 500);
        };

        RetryPolicy retryPolicy = RetryPolicy.builder().maxRetries(0).build();
        CircuitBreaker breaker = new CircuitBreaker(2, Duration.ofSeconds(10), 1);
        ResilientHttpClient client = new ResilientHttpClient(mockClient, retryPolicy, breaker, duration -> {});

        // Attempt 1 fails
        assertThatThrownBy(() -> client.execute(HttpRequest.get("https://api.example.com/down")))
                .isInstanceOf(NetworkException.class);

        // Attempt 2 fails and trips circuit
        assertThatThrownBy(() -> client.execute(HttpRequest.get("https://api.example.com/down")))
                .isInstanceOf(NetworkException.class);

        // Attempt 3 is blocked by CircuitBreaker
        assertThatThrownBy(() -> client.execute(HttpRequest.get("https://api.example.com/down")))
                .isInstanceOf(CircuitBreakerOpenException.class);
    }
}
