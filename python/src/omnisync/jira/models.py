"""Domain models for Jira resources."""

from dataclasses import dataclass


@dataclass(frozen=True)
class JiraIssue:
    """Typed representation of an extracted Jira issue."""

    id: str
    key: str
    summary: str = ""
    status: str = "Unknown"
    issue_type: str = "Unknown"
    priority: str = "None"
    created: str = ""
    updated: str = ""
    assignee: str = "Unassigned"
