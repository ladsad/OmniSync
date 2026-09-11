"""Unit and integration tests for Python HTTP transport layer."""

from http.server import BaseHTTPRequestHandler, HTTPServer
import threading
from typing import Generator
import urllib.error
import pytest

from omnisync.error import (
    AuthenticationError,
    NetworkError,
    RateLimitExceededError,
)
from omnisync.http import (
    DefaultHttpClient,
    HttpMethod,
    HttpRequest,
    HttpResponse,
)


class MockServerHandler(BaseHTTPRequestHandler):
    """Handler for local HTTP test server."""

    def log_message(self, format: str, *args: object) -> None:
        """Suppress stdout log noise."""

    def do_GET(self) -> None:
        if self.path.startswith("/ok"):
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"status": "ok"}')
        elif self.path == "/unauthorized":
            self.send_response(401)
            self.end_headers()
            self.wfile.write(b"Unauthorized access")
        elif self.path == "/rate-limited":
            self.send_response(429)
            self.send_header("Retry-After", "45")
            self.end_headers()
            self.wfile.write(b"Too many requests")
        elif self.path == "/server-error":
            self.send_response(503)
            self.end_headers()
            self.wfile.write(b"Service Unavailable")
        else:
            self.send_response(404)
            self.end_headers()
            self.wfile.write(b"Not Found")

    def do_POST(self) -> None:
        self._echo_method()

    def do_PUT(self) -> None:
        self._echo_method()

    def do_PATCH(self) -> None:
        self._echo_method()

    def do_DELETE(self) -> None:
        self._echo_method()

    def _echo_method(self) -> None:
        content_length = int(self.headers.get("Content-Length", 0))
        if content_length > 0:
            self.rfile.read(content_length)
        body = b'{"echo": true}'
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("X-Echo-Method", self.command)
        self.end_headers()
        self.wfile.write(body)


@pytest.fixture(scope="module")
def mock_server() -> Generator[str, None, None]:
    """Start in-process HTTP server for test suite."""
    server = HTTPServer(("localhost", 0), MockServerHandler)
    host, port = server.server_address
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    yield f"http://{host}:{port}"
    server.shutdown()
    server.server_close()


def test_http_request_and_response_models() -> None:
    req = HttpRequest(
        url="https://api.example.com",
        method=HttpMethod.POST,
        headers={"Content-Type": "application/json"},
        query_params={"limit": "20"},
        body="{}",
        timeout_seconds=15.0,
    )
    assert req.url == "https://api.example.com"
    assert req.method == HttpMethod.POST
    assert req.headers["Content-Type"] == "application/json"
    assert req.query_params["limit"] == "20"
    assert req.body == "{}"
    assert req.timeout_seconds == 15.0

    res = HttpResponse(status_code=200, headers={"Content-Type": "application/json"}, body='{"key":"val"}')
    assert res.is_successful is True
    assert res.get_header("content-type") == "application/json"
    assert res.get_header("CONTENT-TYPE") == "application/json"
    assert res.get_header("missing") is None
    assert res.get_header("") is None

    res_fail = HttpResponse(status_code=500)
    assert res_fail.is_successful is False
    assert res_fail.body == ""


def test_default_http_client_get_success(mock_server: str) -> None:
    client = DefaultHttpClient()
    req = HttpRequest(
        url=f"{mock_server}/ok",
        query_params={"cursor": "abc"},
        headers={"Accept": "application/json"},
    )
    res = client.execute(req)
    assert res.status_code == 200
    assert res.is_successful is True
    assert res.body == '{"status": "ok"}'
    assert res.get_header("content-type") == "application/json"


def test_default_http_client_auth_error(mock_server: str) -> None:
    client = DefaultHttpClient()
    req = HttpRequest(url=f"{mock_server}/unauthorized")
    with pytest.raises(AuthenticationError, match="HTTP 401"):
        client.execute(req)


def test_default_http_client_rate_limit(mock_server: str) -> None:
    client = DefaultHttpClient()
    req = HttpRequest(url=f"{mock_server}/rate-limited")
    with pytest.raises(RateLimitExceededError) as exc_info:
        client.execute(req)
    assert exc_info.value.retry_after_seconds == 45.0

    # Test non-numeric retry-after
    from unittest.mock import MagicMock, patch
    mock_err = urllib.error.HTTPError(
        url="http://example.com",
        code=429,
        msg="Too Many",
        hdrs=MagicMock(items=lambda: [("Retry-After", "not-a-number")]),
        fp=None,
    )
    with patch("urllib.request.urlopen", side_effect=mock_err):
        with pytest.raises(RateLimitExceededError) as exc_nan:
            client.execute(HttpRequest(url="http://example.com"))
        assert exc_nan.value.retry_after_seconds is None


def test_default_http_client_server_and_client_error(mock_server: str) -> None:
    client = DefaultHttpClient()
    with pytest.raises(NetworkError) as exc_503:
        client.execute(HttpRequest(url=f"{mock_server}/server-error"))
    assert exc_503.value.status_code == 503

    with pytest.raises(NetworkError) as exc_404:
        client.execute(HttpRequest(url=f"{mock_server}/not-found"))
    assert exc_404.value.status_code == 404


def test_default_http_client_connection_failure() -> None:
    client = DefaultHttpClient()
    with pytest.raises(NetworkError):
        client.execute(HttpRequest(url="http://localhost:1", timeout_seconds=1.0))


def test_default_http_client_methods(mock_server: str) -> None:
    client = DefaultHttpClient()

    for method in (HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE):
        req = HttpRequest(url=f"{mock_server}/echo", method=method, body="data")
        res = client.execute(req)
        assert res.status_code == 200
        assert res.get_header("x-echo-method") == method.value
