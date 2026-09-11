"""Unit tests for Python JiraConnector."""

from unittest.mock import MagicMock
import pytest

from omnisync.auth import BasicAuthStrategy
from omnisync.error import AuthenticationError, RateLimitExceededError
from omnisync.http import HttpClient, HttpRequest, HttpResponse
from omnisync.jira import JiraConnector


def test_jira_connector_fetch_page_builds_request() -> None:
    mock_client = MagicMock(spec=HttpClient)
    page_json = """
    {
      "startAt": 0,
      "maxResults": 1,
      "total": 2,
      "issues": [
        { "id": "10", "key": "APP-1", "fields": { "summary": "Initial setup" } }
      ]
    }
    """
    mock_client.execute.return_value = HttpResponse(
        status_code=200,
        headers={"Content-Type": "application/json"},
        body=page_json,
    )

    auth = BasicAuthStrategy("user@example.com", "secret")
    connector = JiraConnector(
        base_url="https://testjira.atlassian.net",
        auth_strategy=auth,
        http_client=mock_client,
        page_size=1,
        jql="project=APP",
    )

    page = connector.fetch_page(None)

    assert mock_client.execute.call_count == 1
    call_args: HttpRequest = mock_client.execute.call_args[0][0]
    assert call_args.url == "https://testjira.atlassian.net/rest/api/3/search"
    assert call_args.query_params["startAt"] == "0"
    assert call_args.query_params["maxResults"] == "1"
    assert call_args.query_params["jql"] == "project=APP"
    assert "Authorization" in call_args.headers

    assert page.has_next is True
    assert page.next_cursor == "1"
    assert len(page.records) == 1
    assert page.records[0].key == "APP-1"


def test_jira_connector_paginate_multiple_pages() -> None:
    mock_client = MagicMock(spec=HttpClient)
    page1 = """
    {
      "startAt": 0,
      "maxResults": 2,
      "total": 3,
      "issues": [
        { "id": "1", "key": "K-1", "fields": { "summary": "S1" } },
        { "id": "2", "key": "K-2", "fields": { "summary": "S2" } }
      ]
    }
    """
    page2 = """
    {
      "startAt": 2,
      "maxResults": 2,
      "total": 3,
      "issues": [
        { "id": "3", "key": "K-3", "fields": { "summary": "S3" } }
      ]
    }
    """
    mock_client.execute.side_effect = [
        HttpResponse(status_code=200, body=page1),
        HttpResponse(status_code=200, body=page2),
    ]

    auth = BasicAuthStrategy("u", "p")
    connector = JiraConnector("https://testjira.atlassian.net/", auth, mock_client, page_size=2, jql="")

    keys = [issue.key for issue in connector.paginate()]
    assert keys == ["K-1", "K-2", "K-3"]
    assert mock_client.execute.call_count == 2
    assert connector.base_url == "https://testjira.atlassian.net"


def test_jira_connector_invalid_cursor_fallback() -> None:
    mock_client = MagicMock(spec=HttpClient)
    mock_client.execute.return_value = HttpResponse(
        status_code=200,
        body='{"startAt": 0, "maxResults": 50, "total": 0, "issues": []}',
    )

    auth = BasicAuthStrategy("u", "p")
    connector = JiraConnector("https://jira.com", auth, mock_client)
    page = connector.fetch_page("not-an-int")
    assert page.has_next is False
    assert page.records == []


def test_jira_connector_error_propagation() -> None:
    mock_client = MagicMock(spec=HttpClient)
    mock_client.execute.side_effect = RateLimitExceededError("rate limited", retry_after_seconds=30.0)

    auth = BasicAuthStrategy("u", "p")
    connector = JiraConnector("https://jira.com", auth, mock_client)

    with pytest.raises(RateLimitExceededError):
        connector.fetch_page(None)

    mock_client.execute.side_effect = AuthenticationError("unauthorized")
    with pytest.raises(AuthenticationError):
        connector.fetch_page(None)


def test_jira_connector_validation() -> None:
    auth = BasicAuthStrategy("u", "p")
    with pytest.raises(ValueError, match="base_url must not be empty"):
        JiraConnector("", auth)
