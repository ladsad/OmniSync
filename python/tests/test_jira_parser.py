"""Unit tests for the Jira search response parser."""

import pytest

from omnisync.error import MalformedDataError
from omnisync.jira.parser import parse_jira_search_response


def test_parse_valid_jira_response() -> None:
    payload = """
    {
      "startAt": 0,
      "maxResults": 50,
      "total": 1,
      "issues": [
        {
          "id": "1000",
          "key": "DEV-1",
          "fields": {
            "summary": "Implement OAuth flow",
            "status": { "name": "In Progress" },
            "issuetype": { "name": "Story" },
            "priority": { "name": "Medium" },
            "created": "2026-09-01T08:00:00.000Z",
            "updated": "2026-09-02T09:00:00.000Z",
            "assignee": { "displayName": "Charlie" }
          }
        }
      ]
    }
    """
    res = parse_jira_search_response(payload)
    assert res.start_at == 0
    assert res.max_results == 50
    assert res.total == 1
    assert len(res.issues) == 1

    issue = res.issues[0]
    assert issue.id == "1000"
    assert issue.key == "DEV-1"
    assert issue.summary == "Implement OAuth flow"
    assert issue.status == "In Progress"
    assert issue.issue_type == "Story"
    assert issue.priority == "Medium"
    assert issue.created == "2026-09-01T08:00:00.000Z"
    assert issue.updated == "2026-09-02T09:00:00.000Z"
    assert issue.assignee == "Charlie"


def test_parse_defensive_skipping() -> None:
    payload = """
    {
      "startAt": 10,
      "maxResults": 20,
      "total": 15,
      "issues": [
        {
          "id": "2000",
          "key": "DEV-2",
          "fields": null
        },
        {
          "missing_keys": true
        },
        "not a dict"
      ]
    }
    """
    res = parse_jira_search_response(payload)
    assert res.start_at == 10
    assert res.max_results == 20
    assert res.total == 15
    assert len(res.issues) == 1
    assert res.issues[0].id == "2000"
    assert res.issues[0].summary == ""
    assert res.issues[0].assignee == "Unassigned"


def test_parse_malformed_failures() -> None:
    with pytest.raises(MalformedDataError, match="Empty or null"):
        parse_jira_search_response("")

    with pytest.raises(MalformedDataError, match="Failed to parse"):
        parse_jira_search_response("{invalid json")

    with pytest.raises(MalformedDataError, match="root must be a JSON object"):
        parse_jira_search_response("[]")
