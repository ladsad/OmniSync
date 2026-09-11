"""OmniSync error hierarchy package."""

from omnisync.error.exceptions import (
    AuthenticationError,
    CircuitBreakerOpenError,
    MalformedDataError,
    NetworkError,
    OmniSyncError,
    RateLimitExceededError,
)

__all__ = [
    "AuthenticationError",
    "CircuitBreakerOpenError",
    "MalformedDataError",
    "NetworkError",
    "OmniSyncError",
    "RateLimitExceededError",
]
