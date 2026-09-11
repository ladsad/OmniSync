"""Pagination abstractions for streaming records across multi-page API responses."""

from dataclasses import dataclass, field
from typing import Callable, Generic, Iterator, Optional, TypeVar

T = TypeVar("T")


@dataclass(frozen=True)
class Page(Generic[T]):
    """Represents a single immutable page of records retrieved from a SaaS API."""

    records: list[T] = field(default_factory=list)
    next_cursor: Optional[str] = None
    has_next: bool = False


PageFetcher = Callable[[Optional[str]], Optional[Page[T]]]


class PaginationIterator(Iterator[T], Generic[T]):
    """Iterator providing lazy traversal across paginated endpoints."""

    def __init__(self, fetcher: PageFetcher[T]) -> None:
        """Initialize PaginationIterator.

        Args:
            fetcher: Callable that accepts a cursor and returns a Page.
        """
        self._fetcher = fetcher
        self._next_cursor: Optional[str] = None
        self._has_more_pages = True
        self._buffer: list[T] = []

    def __iter__(self) -> Iterator[T]:
        """Return self as iterator."""
        return self

    def __next__(self) -> T:
        """Return the next record from the buffer, fetching pages when depleted."""
        while not self._buffer and self._has_more_pages:
            page = self._fetcher(self._next_cursor)
            if page is None:
                self._has_more_pages = False
                break
            self._buffer = list(page.records)
            self._next_cursor = page.next_cursor
            self._has_more_pages = page.has_next

        if not self._buffer:
            raise StopIteration
        return self._buffer.pop(0)

    @property
    def next_cursor(self) -> Optional[str]:
        """Return the latest cursor received from the upstream provider."""
        return self._next_cursor


def paginate(fetcher: PageFetcher[T]) -> Iterator[T]:
    """Convenience generator function returning a lazy record iterator.

    Args:
        fetcher: Callable taking cursor string and returning Page[T].

    Returns:
        Iterator yielding individual records.
    """
    return PaginationIterator(fetcher)
