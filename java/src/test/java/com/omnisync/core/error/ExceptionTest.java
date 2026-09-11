package com.omnisync.core.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionTest {

    @Test
    @DisplayName("OmniSyncException supports message and cause")
    void omniSyncExceptionFields() {
        Throwable cause = new RuntimeException("root");
        OmniSyncException ex = new OmniSyncException("failed", cause);

        assertThat(ex.getMessage()).isEqualTo("failed");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("AuthenticationException inherits hierarchy correctly")
    void authenticationExceptionHierarchy() {
        AuthenticationException ex1 = new AuthenticationException("unauthorized");
        Throwable cause = new IllegalStateException("bad token");
        AuthenticationException ex2 = new AuthenticationException("unauthorized", cause);

        assertThat(ex1).isInstanceOf(OmniSyncException.class);
        assertThat(ex1.getMessage()).isEqualTo("unauthorized");
        assertThat(ex2.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("RateLimitExceededException retains retryAfter duration")
    void rateLimitExceededExceptionProperties() {
        RateLimitExceededException withoutDuration = new RateLimitExceededException("rate limited");
        assertThat(withoutDuration.getRetryAfter()).isEmpty();

        Duration duration = Duration.ofSeconds(30);
        RateLimitExceededException withDuration = new RateLimitExceededException("rate limited", duration);
        assertThat(withDuration.getRetryAfter()).contains(duration);

        Throwable cause = new RuntimeException("429 Too Many Requests");
        RateLimitExceededException withCause = new RateLimitExceededException("rate limited", duration, cause);
        assertThat(withCause.getRetryAfter()).contains(duration);
        assertThat(withCause.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("MalformedDataException retains raw payload context")
    void malformedDataExceptionProperties() {
        MalformedDataException withoutPayload = new MalformedDataException("corrupt json");
        assertThat(withoutPayload.getRawPayload()).isEmpty();

        MalformedDataException withPayload = new MalformedDataException("corrupt json", "{\"invalid\":}");
        assertThat(withPayload.getRawPayload()).contains("{\"invalid\":}");

        Throwable cause = new IllegalArgumentException("parse failed");
        MalformedDataException withCause = new MalformedDataException("corrupt json", "{\"invalid\":}", cause);
        assertThat(withCause.getRawPayload()).contains("{\"invalid\":}");
        assertThat(withCause.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("NetworkException retains status code")
    void networkExceptionProperties() {
        NetworkException withoutStatus = new NetworkException("timeout");
        assertThat(withoutStatus.getStatusCode()).isEmpty();

        NetworkException withStatus = new NetworkException("server error", 503);
        assertThat(withStatus.getStatusCode()).hasValue(503);

        Throwable cause = new java.io.IOException("connect reset");
        NetworkException withCause = new NetworkException("server error", 500, cause);
        assertThat(withCause.getStatusCode()).hasValue(500);
        assertThat(withCause.getCause()).isSameAs(cause);
    }
}
