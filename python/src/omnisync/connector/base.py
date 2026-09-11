"""Connector contracts and base implementations for OmniSync."""

from abc import ABC, abstractmethod
from typing import Generic, Iterator, Optional, TypeVar

from omnisync.auth.strategy import AuthStrategy
from omnisync.error.exceptions import OmniSyncError
from omnisync.pagination.paginator import Page, paginate

T = TypeVar("T")


class Connector(ABC, Generic[T]):
    """Standardized connector contract for SaaS integrations."""

    @abstractmethod
    def authenticate(self) -> None:
        """Perform authentication handshakes or verify active credentials.

        Raises:
            OmniSyncError: If authentication fails.
        """

    @abstractmethod
    def fetch_page(self, cursor: Optional[str] = None) -> Page[T]:
        """Fetch a single page of domain items given a pagination cursor.

        Args:
            cursor: Token/offset for target page, or None for initial page.

        Returns:
            Page containing extracted items and next pagination cursor.

        Raises:
            OmniSyncError: On network, authorization, or parsing failures.
        """

    @abstractmethod
    def extract_records(self, page: Page[T]) -> list[T]:
        """Extract and normalize domain records from a retrieved page container.

        Args:
            page: Page object containing records.

        Returns:
            List of domain records.
        """

    @abstractmethod
    def handle_error(self, error: OmniSyncError) -> None:
        """Standard error handler for handling or classifying extraction exceptions.

        Args:
            error: The caught framework error.

        Raises:
            OmniSyncError: By default re-raises the error.
        """

    @abstractmethod
    def paginate(self) -> Iterator[T]:
        """Return an iterator streaming all records across all available pages.

        Returns:
            Iterator yielding items of type T.
        """


class BaseConnector(Connector[T], ABC):
    """Abstract connector implementing common auth delegation and streaming."""

    def __init__(self, auth_strategy: AuthStrategy) -> None:
        """Initialize BaseConnector.

        Args:
            auth_strategy: Authentication strategy to use for requests.

        Raises:
            ValueError: If auth_strategy is None.
        """
        if auth_strategy is None:
            raise ValueError("auth_strategy must not be None")
        self.auth_strategy = auth_strategy

    def authenticate(self) -> None:
        """Delegate authentication to injected AuthStrategy."""
        self.auth_strategy.authenticate()

    def extract_records(self, page: Page[T]) -> list[T]:
        """Extract records safely from page container."""
        if page is None or not page.records:
            return []
        return list(page.records)

    def handle_error(self, error: OmniSyncError) -> None:
        """Default error handler propagating the exception."""
        raise error

    def paginate(self) -> Iterator[T]:
        """Return a lazy streaming iterator across all pages."""
        return paginate(self.fetch_page)
