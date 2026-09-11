"""OmniSync error hierarchy package."""

from omnisync.error.exceptions import (
    AuthenticationError,
    MalformedDataError,
    NetworkError,
    OmniSyncError,
    RateLimitExceededError,
)

__all__ = [
    "AuthenticationError",
    "MalformedDataError",
    "NetworkError",
    "OmniSyncError",
    "RateLimitExceededError",
]
