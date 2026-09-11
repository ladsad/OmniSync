"""Unit tests for the resilience layer."""

import time
from unittest.mock import MagicMock
import pytest

from omnisync.error import (
    AuthenticationError,
    CircuitBreakerOpenError,
    MalformedDataError,
    NetworkError,
    RateLimitExceededError,
)
from omnisync.http import HttpClient, HttpRequest, HttpResponse
from omnisync.resilience import (
    CircuitBreaker,
    CircuitState,
    ResilientHttpClient,
    RetryPolicy,
)


def test_retry_policy_classification() -> None:
    policy = RetryPolicy(max_retries=2)
    assert policy.should_retry(NetworkError("fail", 503), 0) is True
    assert policy.should_retry(RateLimitExceededError("rate limit"), 1) is True

    # Attempts exhausted
    assert policy.should_retry(NetworkError("fail", 503), 2) is False

    # Non-retryable errors
    assert policy.should_retry(AuthenticationError("unauthorized"), 0) is False
    assert policy.should_retry(MalformedDataError("bad json"), 0) is False


def test_retry_policy_delay_computations() -> None:
    policy = RetryPolicy(
        initial_backoff_seconds=0.1,
        backoff_multiplier=2.0,
        max_backoff_seconds=0.5,
        respect_retry_after=True,
    )
    err = NetworkError("timeout")
    assert policy.compute_delay(err, 0) == 0.1
    assert policy.compute_delay(err, 1) == 0.2
    assert policy.compute_delay(err, 2) == 0.4
    assert policy.compute_delay(err, 3) == 0.5  # Capped at max_backoff

    rle = RateLimitExceededError("slow down", retry_after_seconds=0.25)
    assert policy.compute_delay(rle, 0) == 0.25


def test_circuit_breaker_lifecycle() -> None:
    breaker = CircuitBreaker(
        failure_threshold=2,
        recovery_timeout_seconds=0.05,
        success_threshold=1,
    )
    assert breaker.state == CircuitState.CLOSED

    # Non-transient errors ignored
    breaker.record_failure(AuthenticationError("auth"))
    assert breaker.failure_count == 0

    # First transient failure
    breaker.record_failure(NetworkError("500", 500))
    assert breaker.state == CircuitState.CLOSED
    assert breaker.failure_count == 1

    # Second failure trips breaker
    breaker.record_failure(NetworkError("500", 500))
    assert breaker.state == CircuitState.OPEN

    # In OPEN state, fast-fails
    with pytest.raises(CircuitBreakerOpenError):
        breaker.allow_execution()

    # Wait past recovery timeout
    time.sleep(0.06)

    # Transition to HALF_OPEN
    breaker.allow_execution()
    assert breaker.state == CircuitState.HALF_OPEN

    # Failure in HALF_OPEN returns to OPEN
    breaker.record_failure(NetworkError("500", 500))
    assert breaker.state == CircuitState.OPEN

    time.sleep(0.06)
    breaker.allow_execution()
    assert breaker.state == CircuitState.HALF_OPEN

    # Success closes the breaker
    breaker.record_success()
    assert breaker.state == CircuitState.CLOSED
    assert breaker.failure_count == 0

    breaker.reset()
    assert breaker.state == CircuitState.CLOSED


def test_resilient_http_client_retries_and_succeeds() -> None:
    mock_client = MagicMock(spec=HttpClient)
    mock_client.execute.side_effect = [
        NetworkError("connect failure"),
        NetworkError("server error", 503),
        HttpResponse(status_code=200, body="{\"ok\": true}"),
    ]

    sleeps: list[float] = []
    policy = RetryPolicy(max_retries=3, initial_backoff_seconds=0.01)
    resilient_client = ResilientHttpClient(
        underlying_client=mock_client,
        retry_policy=policy,
        sleeper=sleeps.append,
    )

    req = HttpRequest(url="https://api.example.com/data")
    res = resilient_client.execute(req)

    assert res.status_code == 200
    assert mock_client.execute.call_count == 3
    assert len(sleeps) == 2


def test_resilient_http_client_non_retryable_failure() -> None:
    mock_client = MagicMock(spec=HttpClient)
    mock_client.execute.side_effect = AuthenticationError("bad credentials")

    resilient_client = ResilientHttpClient(mock_client)
    with pytest.raises(AuthenticationError):
        resilient_client.execute(HttpRequest(url="https://api.example.com"))

    assert mock_client.execute.call_count == 1


def test_resilient_http_client_trips_circuit_breaker() -> None:
    mock_client = MagicMock(spec=HttpClient)
    mock_client.execute.side_effect = NetworkError("service down", 500)

    breaker = CircuitBreaker(failure_threshold=2, recovery_timeout_seconds=5.0)
    policy = RetryPolicy(max_retries=0)
    resilient_client = ResilientHttpClient(
        underlying_client=mock_client,
        retry_policy=policy,
        circuit_breaker=breaker,
    )

    # Attempt 1
    with pytest.raises(NetworkError):
        resilient_client.execute(HttpRequest(url="https://api.example.com"))

    # Attempt 2 trips breaker
    with pytest.raises(NetworkError):
        resilient_client.execute(HttpRequest(url="https://api.example.com"))

    # Attempt 3 blocked by circuit breaker
    with pytest.raises(CircuitBreakerOpenError):
        resilient_client.execute(HttpRequest(url="https://api.example.com"))


def test_resilient_http_client_validation() -> None:
    with pytest.raises(ValueError, match="underlying_client must not be None"):
        ResilientHttpClient(underlying_client=None)  # type: ignore[arg-type]
