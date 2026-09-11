"""Unit tests for the Connector abstraction."""

from typing import Optional
from unittest.mock import MagicMock
import pytest

from omnisync.auth import AuthStrategy
from omnisync.connector import BaseConnector
from omnisync.error import AuthenticationError, OmniSyncError
from omnisync.pagination import Page


class StubConnector(BaseConnector[dict]):
    """Test concrete connector implementation."""

    def __init__(self, auth_strategy: AuthStrategy) -> None:
        super().__init__(auth_strategy)
        self.call_count = 0

    def fetch_page(self, cursor: Optional[str] = None) -> Page[dict]:
        self.call_count += 1
        if cursor is None:
            return Page(records=[{"id": 1}, {"id": 2}], next_cursor="page-2", has_next=True)
        return Page(records=[{"id": 3}], next_cursor=None, has_next=False)


def test_connector_delegates_authentication() -> None:
    mock_auth = MagicMock(spec=AuthStrategy)
    connector = StubConnector(mock_auth)

    connector.authenticate()
    mock_auth.authenticate.assert_called_once()
    assert connector.auth_strategy is mock_auth


def test_connector_propagates_authentication_error() -> None:
    mock_auth = MagicMock(spec=AuthStrategy)
    mock_auth.authenticate.side_effect = AuthenticationError("expired")
    connector = StubConnector(mock_auth)

    with pytest.raises(AuthenticationError, match="expired"):
        connector.authenticate()


def test_connector_extract_records() -> None:
    mock_auth = MagicMock(spec=AuthStrategy)
    connector = StubConnector(mock_auth)

    page = Page(records=[{"id": 10}])
    assert connector.extract_records(page) == [{"id": 10}]
    assert connector.extract_records(None) == []  # type: ignore[arg-type]


def test_connector_paginate_streams_records() -> None:
    mock_auth = MagicMock(spec=AuthStrategy)
    connector = StubConnector(mock_auth)

    records = list(connector.paginate())
    assert records == [{"id": 1}, {"id": 2}, {"id": 3}]
    assert connector.call_count == 2


def test_connector_handle_error_raises() -> None:
    mock_auth = MagicMock(spec=AuthStrategy)
    connector = StubConnector(mock_auth)
    error = OmniSyncError("downstream failure")

    with pytest.raises(OmniSyncError, match="downstream failure"):
        connector.handle_error(error)


def test_connector_requires_auth_strategy() -> None:
    with pytest.raises(ValueError, match="auth_strategy must not be None"):
        StubConnector(None)  # type: ignore[arg-type]
