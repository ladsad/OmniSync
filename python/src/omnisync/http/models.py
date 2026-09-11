"""HTTP request and response models for OmniSync transport."""

from dataclasses import dataclass, field
from enum import Enum
from typing import Optional


class HttpMethod(str, Enum):
    """Supported HTTP request methods."""

    GET = "GET"
    POST = "POST"
    PUT = "PUT"
    PATCH = "PATCH"
    DELETE = "DELETE"


@dataclass(frozen=True)
class HttpRequest:
    """Immutable model representing an outbound HTTP request."""

    url: str
    method: HttpMethod = HttpMethod.GET
    headers: dict[str, str] = field(default_factory=dict)
    query_params: dict[str, str] = field(default_factory=dict)
    body: Optional[str] = None
    timeout_seconds: float = 30.0


@dataclass(frozen=True)
class HttpResponse:
    """Immutable model representing an inbound HTTP response."""

    status_code: int
    headers: dict[str, str] = field(default_factory=dict)
    body: str = ""

    def __post_init__(self) -> None:
        """Normalize header keys to lowercase."""
        normalized = {k.lower(): v for k, v in self.headers.items() if k is not None}
        object.__setattr__(self, "headers", normalized)

    @property
    def is_successful(self) -> bool:
        """Indicate whether status code is in 2xx range."""
        return 200 <= self.status_code < 300

    def get_header(self, name: str) -> Optional[str]:
        """Retrieve header value case-insensitively.

        Args:
            name: Header name.

        Returns:
            Header value string if present, None otherwise.
        """
        if not name:
            return None
        return self.headers.get(name.lower())
