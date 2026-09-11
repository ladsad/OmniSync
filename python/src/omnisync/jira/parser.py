"""Defensive JSON parser for Jira REST search responses."""

from dataclasses import dataclass, field
import json
from typing import Any, Optional

from omnisync.error.exceptions import MalformedDataError
from omnisync.jira.models import JiraIssue


@dataclass(frozen=True)
class JiraSearchResult:
    """Pagination metadata and issues parsed from Jira search API."""

    start_at: int
    max_results: int
    total: int
    issues: list[JiraIssue] = field(default_factory=list)


def parse_jira_search_response(raw_json: str) -> JiraSearchResult:
    """Parse raw JSON from Jira search endpoint defensively.

    Args:
        raw_json: Raw JSON response payload string.

    Returns:
        JiraSearchResult containing pagination metadata and parsed issues.

    Raises:
        MalformedDataError: If JSON syntax is invalid or root is not an object.
    """
    if not raw_json or not raw_json.strip():
        raise MalformedDataError("Empty or null Jira JSON payload", raw_payload=raw_json)

    try:
        data = json.loads(raw_json)
    except Exception as exc:
        raise MalformedDataError(
            f"Failed to parse Jira response: {exc}",
            raw_payload=raw_json,
        ) from exc

    if not isinstance(data, dict):
        raise MalformedDataError(
            "Jira response root must be a JSON object",
            raw_payload=raw_json,
        )

    start_at = int(data.get("startAt", 0))
    max_results = int(data.get("maxResults", 50))
    total = int(data.get("total", 0))

    issues_raw = data.get("issues", [])
    issues: list[JiraIssue] = []

    if isinstance(issues_raw, list):
        for item in issues_raw:
            issue = _parse_single_issue(item)
            if issue is not None:
                issues.append(issue)

    return JiraSearchResult(
        start_at=start_at,
        max_results=max_results,
        total=total,
        issues=issues,
    )


def _parse_single_issue(item: Any) -> Optional[JiraIssue]:
    """Parse individual issue object, returning None if mandatory IDs missing."""
    if not isinstance(item, dict):
        return None

    issue_id = item.get("id")
    key = item.get("key")
    if not issue_id or not key:
        return None

    fields = item.get("fields")
    if not isinstance(fields, dict):
        fields = {}

    summary = str(fields.get("summary") or "")

    status_obj = fields.get("status")
    status = status_obj.get("name", "Unknown") if isinstance(status_obj, dict) else "Unknown"

    type_obj = fields.get("issuetype")
    issue_type = type_obj.get("name", "Unknown") if isinstance(type_obj, dict) else "Unknown"

    priority_obj = fields.get("priority")
    priority = priority_obj.get("name", "None") if isinstance(priority_obj, dict) else "None"

    created = str(fields.get("created") or "")
    updated = str(fields.get("updated") or "")

    assignee_obj = fields.get("assignee")
    assignee = (
        assignee_obj.get("displayName", "Unassigned")
        if isinstance(assignee_obj, dict)
        else "Unassigned"
    )

    return JiraIssue(
        id=str(issue_id),
        key=str(key),
        summary=summary,
        status=status,
        issue_type=issue_type,
        priority=priority,
        created=created,
        updated=updated,
        assignee=assignee,
    )
