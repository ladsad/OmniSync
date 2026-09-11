"""Structured exception hierarchy for OmniSync."""

from typing import Optional


class OmniSyncError(Exception):
    """Base exception for all OmniSync framework errors."""

    def __init__(self, message: str) -> None:
        """Initialize base OmniSync error.

        Args:
            message: Descriptive failure message.
        """
        super().__init__(message)
        self.message = message


class AuthenticationError(OmniSyncError):
    """Raised when authentication or credential validation fails against a provider."""


class RateLimitExceededError(OmniSyncError):
    """Raised when an external API rate limit is exceeded (e.g. HTTP 429)."""

    def __init__(
        self,
        message: str,
        retry_after_seconds: Optional[float] = None,
    ) -> None:
        """Initialize rate limit error.

        Args:
            message: Descriptive failure message.
            retry_after_seconds: Recommended delay in seconds before retrying, if known.
        """
        super().__init__(message)
        self.retry_after_seconds = retry_after_seconds


class MalformedDataError(OmniSyncError):
    """Raised when response payload cannot be parsed or does not match expected schema."""

    def __init__(
        self,
        message: str,
        raw_payload: Optional[str] = None,
    ) -> None:
        """Initialize malformed data error.

        Args:
            message: Descriptive failure message.
            raw_payload: Raw payload snippet that caused parsing failure, if available.
        """
        super().__init__(message)
        self.raw_payload = raw_payload


class NetworkError(OmniSyncError):
    """Raised when low-level transport errors occur (timeouts, DNS, connection drops, 5xx)."""

    def __init__(
        self,
        message: str,
        status_code: Optional[int] = None,
    ) -> None:
        """Initialize network error.

        Args:
            message: Descriptive failure message.
            status_code: HTTP status code if error is associated with a response, else None.
        """
        super().__init__(message)
        self.status_code = status_code


class CircuitBreakerOpenError(OmniSyncError):
    """Raised when requests are short-circuited because a circuit breaker is in OPEN state."""

    def __init__(
        self,
        message: str,
        remaining_timeout_seconds: Optional[float] = None,
    ) -> None:
        """Initialize circuit breaker error.

        Args:
            message: Descriptive failure message.
            remaining_timeout_seconds: Estimated seconds remaining before circuit probe is allowed.
        """
        super().__init__(message)
        self.remaining_timeout_seconds = remaining_timeout_seconds

