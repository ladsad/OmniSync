"""Retry policy and exponential backoff calculator."""

from dataclasses import dataclass
from typing import Optional

from omnisync.error.exceptions import NetworkError, RateLimitExceededError


@dataclass(frozen=True)
class RetryPolicy:
    """Policy configuring maximum retries, exponential backoff, and rate limit handling."""

    max_retries: int = 3
    initial_backoff_seconds: float = 0.5
    max_backoff_seconds: float = 30.0
    backoff_multiplier: float = 2.0
    respect_retry_after: bool = True

    def should_retry(self, error: Exception, attempt: int) -> bool:
        """Determine if a caught error should be retried at the given attempt count.

        Args:
            error: The caught exception.
            attempt: Zero-based attempt counter (0 is after first failure).

        Returns:
            True if attempt < max_retries and error is transient.
        """
        if attempt >= self.max_retries:
            return False
        return isinstance(error, (NetworkError, RateLimitExceededError))

    def compute_delay(self, error: Exception, attempt: int) -> float:
        """Compute delay in seconds before performing next retry attempt.

        Args:
            error: The caught exception.
            attempt: Zero-based attempt counter.

        Returns:
            Delay duration in seconds.
        """
        if self.respect_retry_after and isinstance(error, RateLimitExceededError):
            if error.retry_after_seconds is not None:
                return min(self.max_backoff_seconds, error.retry_after_seconds)

        calculated = self.initial_backoff_seconds * (self.backoff_multiplier ** attempt)
        return min(self.max_backoff_seconds, calculated)
