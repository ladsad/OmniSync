"""Unit tests for the OmniSync error hierarchy."""

from omnisync.error import (
    AuthenticationError,
    MalformedDataError,
    NetworkError,
    OmniSyncError,
    RateLimitExceededError,
)


def test_omnisync_error_base() -> None:
    err = OmniSyncError("base error")
    assert str(err) == "base error"
    assert err.message == "base error"
    assert isinstance(err, Exception)


def test_authentication_error() -> None:
    err = AuthenticationError("unauthorized request")
    assert isinstance(err, OmniSyncError)
    assert err.message == "unauthorized request"


def test_rate_limit_exceeded_error() -> None:
    err_default = RateLimitExceededError("rate limit reached")
    assert err_default.retry_after_seconds is None

    err_custom = RateLimitExceededError("too many requests", retry_after_seconds=45.5)
    assert isinstance(err_custom, OmniSyncError)
    assert err_custom.retry_after_seconds == 45.5


def test_malformed_data_error() -> None:
    err_default = MalformedDataError("corrupt response")
    assert err_default.raw_payload is None

    err_custom = MalformedDataError("corrupt response", raw_payload="<not json>")
    assert isinstance(err_custom, OmniSyncError)
    assert err_custom.raw_payload == "<not json>"


def test_network_error() -> None:
    err_default = NetworkError("connection refused")
    assert err_default.status_code is None

    err_custom = NetworkError("bad gateway", status_code=502)
    assert isinstance(err_custom, OmniSyncError)
    assert err_custom.status_code == 502
