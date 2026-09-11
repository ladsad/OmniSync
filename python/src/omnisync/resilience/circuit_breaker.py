"""Circuit breaker state machine preventing cascading system failures."""

from enum import Enum
import time
from typing import Optional

from omnisync.error.exceptions import (
    CircuitBreakerOpenError,
    NetworkError,
    RateLimitExceededError,
)


class CircuitState(str, Enum):
    """Possible operating states for the circuit breaker."""

    CLOSED = "CLOSED"
    OPEN = "OPEN"
    HALF_OPEN = "HALF_OPEN"


class CircuitBreaker:
    """State machine governing requests based on upstream failure rates."""

    def __init__(
        self,
        failure_threshold: int = 5,
        recovery_timeout_seconds: float = 30.0,
        success_threshold: int = 2,
    ) -> None:
        """Initialize CircuitBreaker.

        Args:
            failure_threshold: Consecutive failures to trip circuit to OPEN.
            recovery_timeout_seconds: Duration circuit stays OPEN before probe is allowed.
            success_threshold: Consecutive successes in HALF_OPEN to return to CLOSED.
        """
        self.failure_threshold = max(1, failure_threshold)
        self.recovery_timeout_seconds = max(0.0, recovery_timeout_seconds)
        self.success_threshold = max(1, success_threshold)

        self.state = CircuitState.CLOSED
        self.failure_count = 0
        self.half_open_success_count = 0
        self.last_failure_time: Optional[float] = None

    def allow_execution(self) -> None:
        """Verify circuit allows execution or raise CircuitBreakerOpenError.

        Raises:
            CircuitBreakerOpenError: When circuit is in OPEN state and recovery window has not elapsed.
        """
        now = time.monotonic()

        if self.state == CircuitState.OPEN:
            elapsed = now - (self.last_failure_time or 0.0)
            if elapsed >= self.recovery_timeout_seconds:
                self.state = CircuitState.HALF_OPEN
                self.half_open_success_count = 0
            else:
                remaining = self.recovery_timeout_seconds - elapsed
                raise CircuitBreakerOpenError(
                    "Circuit breaker is OPEN. Requests short-circuited.",
                    remaining_timeout_seconds=remaining,
                )

    def record_success(self) -> None:
        """Record successful execution."""
        if self.state == CircuitState.HALF_OPEN:
            self.half_open_success_count += 1
            if self.half_open_success_count >= self.success_threshold:
                self.state = CircuitState.CLOSED
                self.failure_count = 0
                self.half_open_success_count = 0
        elif self.state == CircuitState.CLOSED:
            self.failure_count = 0

    def record_failure(self, error: Exception) -> None:
        """Record an execution failure, updating state if threshold exceeded."""
        if not isinstance(error, (NetworkError, RateLimitExceededError)):
            return

        self.last_failure_time = time.monotonic()

        if self.state == CircuitState.HALF_OPEN:
            self.state = CircuitState.OPEN
            self.failure_count = self.failure_threshold
            self.half_open_success_count = 0
        elif self.state == CircuitState.CLOSED:
            self.failure_count += 1
            if self.failure_count >= self.failure_threshold:
                self.state = CircuitState.OPEN

    def reset(self) -> None:
        """Reset circuit breaker to initial CLOSED state."""
        self.state = CircuitState.CLOSED
        self.failure_count = 0
        self.half_open_success_count = 0
        self.last_failure_time = None
