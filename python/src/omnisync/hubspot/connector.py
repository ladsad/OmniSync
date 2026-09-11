"""Concrete HubSpot connector for querying CRM objects via REST API v3."""

from typing import Optional

from omnisync.auth.strategy import AuthStrategy
from omnisync.connector.base import BaseConnector
from omnisync.http import DefaultHttpClient, HttpClient, HttpMethod, HttpRequest
from omnisync.hubspot.models import HubSpotContact
from omnisync.hubspot.parser import parse_hubspot_contact_response
from omnisync.pagination.paginator import Page


class HubSpotConnector(BaseConnector[HubSpotContact]):
    """Connector for querying CRM contacts from HubSpot REST API v3."""

    DEFAULT_BASE_URL = "https://api.hubapi.com"
    DEFAULT_OBJECT_TYPE = "contacts"
    DEFAULT_PAGE_SIZE = 50
    DEFAULT_PROPERTIES = "email,firstname,lastname,company,phone"

    def __init__(
        self,
        auth_strategy: AuthStrategy,
        base_url: str = DEFAULT_BASE_URL,
        http_client: Optional[HttpClient] = None,
        object_type: str = DEFAULT_OBJECT_TYPE,
        page_size: int = DEFAULT_PAGE_SIZE,
        properties: str = DEFAULT_PROPERTIES,
    ) -> None:
        """Initialize HubSpotConnector.

        Args:
            auth_strategy: Injected authentication strategy (typically OAuth2Strategy).
            base_url: Base URL for HubSpot API.
            http_client: Optional HTTP transport client.
            object_type: CRM object type to extract (defaults to 'contacts').
            page_size: Maximum records per page batch.
            properties: Comma-separated CRM property keys.

        Raises:
            ValueError: If auth_strategy or base_url is not provided.
        """
        if not base_url:
            raise ValueError("base_url must not be empty")
        super().__init__(auth_strategy)
        self.base_url = base_url.rstrip("/")
        self.http_client = http_client or DefaultHttpClient()
        self.object_type = object_type if object_type else self.DEFAULT_OBJECT_TYPE
        self.page_size = page_size if page_size > 0 else self.DEFAULT_PAGE_SIZE
        self.properties = properties if properties else self.DEFAULT_PROPERTIES

    def fetch_page(self, cursor: Optional[str] = None) -> Page[HubSpotContact]:
        """Fetch a single page of HubSpot CRM contacts using cursor-based pagination."""
        query_params = {
            "limit": str(self.page_size),
            "properties": self.properties,
            "archived": "false",
        }
        if cursor and cursor.strip():
            query_params["after"] = cursor.strip()

        request = HttpRequest(
            url=f"{self.base_url}/crm/v3/objects/{self.object_type}",
            method=HttpMethod.GET,
            headers=self.auth_strategy.get_auth_headers(),
            query_params=query_params,
        )

        response = self.http_client.execute(request)
        result = parse_hubspot_contact_response(response.body)

        return Page(
            records=result.results,
            next_cursor=result.next_after,
            has_next=result.has_more,
        )
