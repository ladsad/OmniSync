"""Concrete Jira connector for extracting issues via Jira REST API."""

from typing import Optional

from omnisync.auth.strategy import AuthStrategy
from omnisync.connector.base import BaseConnector
from omnisync.http import DefaultHttpClient, HttpClient, HttpMethod, HttpRequest
from omnisync.jira.models import JiraIssue
from omnisync.jira.parser import parse_jira_search_response
from omnisync.pagination.paginator import Page


class JiraConnector(BaseConnector[JiraIssue]):
    """Connector for querying issues from Atlassian Jira REST API."""

    DEFAULT_PAGE_SIZE = 50
    DEFAULT_JQL = "ORDER BY created DESC"
    DEFAULT_FIELDS = "summary,status,issuetype,priority,created,updated,assignee"

    def __init__(
        self,
        base_url: str,
        auth_strategy: AuthStrategy,
        http_client: Optional[HttpClient] = None,
        page_size: int = DEFAULT_PAGE_SIZE,
        jql: str = DEFAULT_JQL,
        fields: str = DEFAULT_FIELDS,
    ) -> None:
        """Initialize JiraConnector.

        Args:
            base_url: Jira base URL (e.g. https://domain.atlassian.net).
            auth_strategy: Injected authentication strategy.
            http_client: Optional HTTP transport client (defaults to DefaultHttpClient).
            page_size: Maximum records per page (maxResults).
            jql: Jira Query Language filter string.
            fields: Comma-separated list of issue fields to retrieve.

        Raises:
            ValueError: If base_url or auth_strategy is not provided.
        """
        if not base_url:
            raise ValueError("base_url must not be empty")
        super().__init__(auth_strategy)
        self.base_url = base_url.rstrip("/")
        self.http_client = http_client or DefaultHttpClient()
        self.page_size = page_size if page_size > 0 else self.DEFAULT_PAGE_SIZE
        self.jql = jql if jql is not None else self.DEFAULT_JQL
        self.fields = fields if fields is not None else self.DEFAULT_FIELDS

    def fetch_page(self, cursor: Optional[str] = None) -> Page[JiraIssue]:
        """Fetch a single page of Jira issues using startAt offset pagination."""
        start_at = 0
        if cursor:
            try:
                start_at = int(cursor.strip())
            except ValueError:
                start_at = 0

        query_params = {
            "startAt": str(start_at),
            "maxResults": str(self.page_size),
            "fields": self.fields,
        }
        if self.jql.strip():
            query_params["jql"] = self.jql

        request = HttpRequest(
            url=f"{self.base_url}/rest/api/3/search",
            method=HttpMethod.GET,
            headers=self.auth_strategy.get_auth_headers(),
            query_params=query_params,
        )

        response = self.http_client.execute(request)
        result = parse_jira_search_response(response.body)

        next_start_at = result.start_at + len(result.issues)
        has_next = next_start_at < result.total and len(result.issues) > 0
        next_cursor = str(next_start_at) if has_next else None

        return Page(
            records=result.issues,
            next_cursor=next_cursor,
            has_next=has_next,
        )
