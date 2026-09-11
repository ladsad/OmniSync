"""HTTP client abstractions and standard implementation."""

from abc import ABC, abstractmethod
import urllib.error
import urllib.parse
import urllib.request
from typing import Optional

from omnisync.error.exceptions import (
    AuthenticationError,
    NetworkError,
    OmniSyncError,
    RateLimitExceededError,
)
from omnisync.http.models import HttpRequest, HttpResponse


class HttpClient(ABC):
    """Abstract contract for executing HTTP requests."""

    @abstractmethod
    def execute(self, request: HttpRequest) -> HttpResponse:
        """Execute request and return normalized response.

        Args:
            request: HttpRequest model.

        Returns:
            HttpResponse model.

        Raises:
            AuthenticationError: On 401/403 responses.
            RateLimitExceededError: On 429 responses.
            NetworkError: On transport/connection/timeout failures or 5xx/4xx errors.
            OmniSyncError: On other protocol failures.
        """


class DefaultHttpClient(HttpClient):
    """Default HTTP client powered by Python's standard urllib."""

    def __init__(self, timeout_seconds: float = 30.0) -> None:
        """Initialize DefaultHttpClient.

        Args:
            timeout_seconds: Global fallback timeout for socket operations.
        """
        self.default_timeout = timeout_seconds

    def execute(self, request: HttpRequest) -> HttpResponse:
        """Execute HTTP request with error translation."""
        url = self._build_url(request.url, request.query_params)
        data = request.body.encode("utf-8") if request.body is not None else None

        req = urllib.request.Request(
            url=url,
            data=data,
            headers=request.headers,
            method=request.method.value,
        )

        timeout = request.timeout_seconds or self.default_timeout

        try:
            with urllib.request.urlopen(req, timeout=timeout) as response:
                body = response.read().decode("utf-8")
                headers = dict(response.headers.items())
                return HttpResponse(
                    status_code=response.status,
                    headers=headers,
                    body=body,
                )
        except urllib.error.HTTPError as exc:
            body = exc.read().decode("utf-8") if exc.fp else ""
            headers = dict(exc.headers.items())
            self._handle_http_error(exc.code, headers, body)
        except (urllib.error.URLError, TimeoutError) as exc:
            raise NetworkError(f"Network transport error: {exc}") from exc

    def _handle_http_error(self, code: int, headers: dict[str, str], body: str) -> None:
        """Translate non-2xx HTTP status codes to OmniSync exceptions."""
        normalized_headers = {k.lower(): v for k, v in headers.items()}
        if code in (401, 403):
            raise AuthenticationError(f"Authentication failed (HTTP {code}): {body}")

        if code == 429:
            retry_after_str = normalized_headers.get("retry-after")
            retry_after_val: Optional[float] = None
            if retry_after_str:
                try:
                    retry_after_val = float(retry_after_str.strip())
                except ValueError:
                    retry_after_val = None
            raise RateLimitExceededError(
                f"Rate limit exceeded (HTTP 429): {body}",
                retry_after_seconds=retry_after_val,
            )

        if code >= 500:
            raise NetworkError(
                f"Upstream server error (HTTP {code}): {body}",
                status_code=code,
            )

        if code >= 400:
            raise NetworkError(
                f"HTTP client error (HTTP {code}): {body}",
                status_code=code,
            )

    def _build_url(self, base_url: str, params: dict[str, str]) -> str:
        """Append encoded query parameters to the URL."""
        if not params:
            return base_url
        query = urllib.parse.urlencode(params)
        separator = "&" if "?" in base_url else "?"
        return f"{base_url}{separator}{query}"
