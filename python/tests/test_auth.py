"""Unit tests for authentication strategies."""

import base64
from datetime import datetime, timedelta, timezone
import pytest

from omnisync.auth import BasicAuthStrategy, OAuth2Strategy
from omnisync.error import AuthenticationError


def test_basic_auth_success() -> None:
    strategy = BasicAuthStrategy("user@example.com", "mySecretToken")
    strategy.authenticate()

    headers = strategy.get_auth_headers()
    assert "Authorization" in headers
    encoded = base64.b64encode(b"user@example.com:mySecretToken").decode("utf-8")
    assert headers["Authorization"] == f"Basic {encoded}"
    assert strategy.is_expired() is False

    strategy.refresh()
    assert strategy.get_auth_headers() == headers


def test_basic_auth_invalid_credentials() -> None:
    empty_user = BasicAuthStrategy("", "pass")
    with pytest.raises(AuthenticationError, match="Username and password must not be empty"):
        empty_user.authenticate()

    empty_pass = BasicAuthStrategy("user", "")
    with pytest.raises(AuthenticationError, match="Username and password must not be empty"):
        empty_pass.get_auth_headers()


def test_oauth2_strategy_success() -> None:
    strategy = OAuth2Strategy(
        client_id="client_123",
        client_secret="secret_abc",
        token_endpoint="https://auth.example.com/oauth/token",
    )
    future = datetime.now(timezone.utc) + timedelta(hours=1)
    strategy.set_tokens(
        access_token="initial_access",
        refresh_token="initial_refresh",
        expires_at=future,
    )

    assert strategy.is_expired() is False
    headers = strategy.get_auth_headers()
    assert headers == {"Authorization": "Bearer initial_access"}


def test_oauth2_strategy_expired_and_refresh() -> None:
    strategy = OAuth2Strategy(
        client_id="client_123",
        client_secret="secret_abc",
        token_endpoint="https://auth.example.com/oauth/token",
    )
    past = datetime.now(timezone.utc) - timedelta(minutes=5)
    strategy.set_tokens(
        access_token="expired_token",
        refresh_token="valid_refresh",
        expires_at=past,
    )

    assert strategy.is_expired() is True
    headers = strategy.get_auth_headers()
    assert strategy.is_expired() is False
    assert headers == {"Authorization": "Bearer refreshed_valid_refresh"}
    assert strategy.access_token == "refreshed_valid_refresh"


def test_oauth2_strategy_no_expires_at_and_token_refresh_flow() -> None:
    strategy = OAuth2Strategy(
        client_id="client_123",
        client_secret="secret_abc",
        token_endpoint="https://auth.example.com/oauth/token",
    )
    strategy.set_tokens(access_token="tok", refresh_token="ref", expires_at=None)
    assert strategy.is_expired() is False

    strategy_ref_only = OAuth2Strategy(
        client_id="client_123",
        client_secret="secret_abc",
        token_endpoint="https://auth.example.com/oauth/token",
    )
    strategy_ref_only.set_tokens(access_token=None, refresh_token="ref_only")
    strategy_ref_only.authenticate()
    assert strategy_ref_only.access_token == "refreshed_ref_only"


def test_oauth2_strategy_failures() -> None:
    no_endpoint = OAuth2Strategy(client_id="id", client_secret="sec", token_endpoint="")
    with pytest.raises(AuthenticationError, match="valid client_id and token_endpoint"):
        no_endpoint.authenticate()

    no_tokens = OAuth2Strategy(client_id="id", client_secret="sec", token_endpoint="https://auth.example.com")
    with pytest.raises(AuthenticationError, match="No OAuth2 access token or refresh token"):
        no_tokens.authenticate()

    with pytest.raises(AuthenticationError, match="refresh token is absent"):
        no_tokens.refresh()
