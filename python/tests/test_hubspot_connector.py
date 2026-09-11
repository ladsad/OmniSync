"""Unit tests for Python HubSpotConnector."""

from unittest.mock import MagicMock
import pytest

from omnisync.auth import OAuth2Strategy
from omnisync.error import AuthenticationError, RateLimitExceededError
from omnisync.http import HttpClient, HttpRequest, HttpResponse
from omnisync.hubspot import HubSpotConnector


def create_mock_auth() -> OAuth2Strategy:
    auth = OAuth2Strategy(
        client_id="client",
        client_secret="secret",
        token_endpoint="https://api.hubapi.com/oauth/v1/token",
    )
    auth.set_tokens(access_token="test_bearer_token", refresh_token="ref")
    return auth


def test_hubspot_connector_initial_fetch() -> None:
    mock_client = MagicMock(spec=HttpClient)
    page_json = """
    {
      "results": [
        { "id": "1", "properties": { "email": "clark@dailyplanet.com" } }
      ],
      "paging": {
        "next": { "after": "c-2" }
      }
    }
    """
    mock_client.execute.return_value = HttpResponse(status_code=200, body=page_json)

    auth = create_mock_auth()
    connector = HubSpotConnector(
        auth_strategy=auth,
        base_url="https://api.hubapi.com",
        http_client=mock_client,
        object_type="contacts",
        page_size=10,
        properties="email",
    )

    page = connector.fetch_page(None)

    assert mock_client.execute.call_count == 1
    call_args: HttpRequest = mock_client.execute.call_args[0][0]
    assert call_args.url == "https://api.hubapi.com/crm/v3/objects/contacts"
    assert call_args.query_params["limit"] == "10"
    assert call_args.query_params["properties"] == "email"
    assert "after" not in call_args.query_params
    assert call_args.headers == {"Authorization": "Bearer test_bearer_token"}

    assert page.has_next is True
    assert page.next_cursor == "c-2"
    assert len(page.records) == 1
    assert page.records[0].email == "clark@dailyplanet.com"


def test_hubspot_connector_paginate_multiple_pages() -> None:
    mock_client = MagicMock(spec=HttpClient)
    page1 = """
    {
      "results": [
        { "id": "101", "properties": { "email": "u1@test.com" } }
      ],
      "paging": {
        "next": { "after": "c-102" }
      }
    }
    """
    page2 = """
    {
      "results": [
        { "id": "102", "properties": { "email": "u2@test.com" } }
      ]
    }
    """
    mock_client.execute.side_effect = [
        HttpResponse(status_code=200, body=page1),
        HttpResponse(status_code=200, body=page2),
    ]

    auth = create_mock_auth()
    connector = HubSpotConnector(auth_strategy=auth, http_client=mock_client)

    emails = [c.email for c in connector.paginate()]
    assert emails == ["u1@test.com", "u2@test.com"]
    assert mock_client.execute.call_count == 2
    assert connector.base_url == "https://api.hubapi.com"
    assert connector.page_size == 50


def test_hubspot_connector_error_propagation() -> None:
    mock_client = MagicMock(spec=HttpClient)
    mock_client.execute.side_effect = RateLimitExceededError("Rate limit exceeded", retry_after_seconds=10.0)

    auth = create_mock_auth()
    connector = HubSpotConnector(auth_strategy=auth, http_client=mock_client)

    with pytest.raises(RateLimitExceededError):
        connector.fetch_page(None)

    mock_client.execute.side_effect = AuthenticationError("Unauthorized")
    with pytest.raises(AuthenticationError):
        connector.fetch_page(None)


def test_hubspot_connector_validation() -> None:
    auth = create_mock_auth()
    with pytest.raises(ValueError, match="base_url must not be empty"):
        HubSpotConnector(auth_strategy=auth, base_url="")
