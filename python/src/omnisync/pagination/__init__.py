"""OmniSync pagination package."""

from omnisync.pagination.paginator import (
    Page,
    PageFetcher,
    PaginationIterator,
    paginate,
)

__all__ = [
    "Page",
    "PageFetcher",
    "PaginationIterator",
    "paginate",
]
