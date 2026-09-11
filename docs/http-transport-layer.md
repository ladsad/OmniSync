# HTTP Transport Layer

## Overview

The HTTP transport layer encapsulates low-level REST networking, request construction, response handling, and protocol error mapping. It enforces a strict separation of concerns between HTTP transport, JSON serialization/parsing, and connector business logic.

## Architecture

```
Connector / AuthStrategy
          │
          ▼
    HttpRequest
          │
          ▼
   HttpClient.execute()
          │
          ▼
  DefaultHttpClient (Java: java.net.http.HttpClient / Python: urllib.request)
          │
          ├─► 2xx: HttpResponse
          ├─► 401/403: AuthenticationException / AuthenticationError
          ├─► 429: RateLimitExceededException / RateLimitExceededError (with Retry-After)
          ├─► 5xx/4xx: NetworkException / NetworkError (with status code)
          └─► Socket/Timeout: NetworkException / NetworkError
```

## Request & Response Models

### 1. `HttpRequest`
- **Fields:**
  - `url`: Target endpoint URL.
  - `method`: HTTP verb (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`).
  - `headers`: Key-value HTTP request headers.
  - `queryParams` / `query_params`: Key-value query parameters automatically encoded into the request URI.
  - `body`: Optional payload string.
  - `timeout` / `timeout_seconds`: Request-level timeout (defaults to 30 seconds).

### 2. `HttpResponse`
- **Fields:**
  - `statusCode` / `status_code`: HTTP status code integer.
  - `headers`: Case-insensitively accessible map/dictionary of response headers.
  - `body`: Raw string payload returned by the server.
- **Convenience Methods:**
  - `isSuccessful()` / `is_successful`: Checks if status code falls within `[200, 299]`.
  - `getHeader(name)` / `get_header(name)`: Case-insensitive header lookup.

## Client Implementations

### `DefaultHttpClient`
- **Java:** Backed by standard `java.net.http.HttpClient` (Java 11+ built-in, zero third-party dependencies). Handles UTF-8 body conversion, timeouts, query encoding, and automatic redirection.
- **Python:** Backed by standard library `urllib.request` (zero third-party dependencies). Handles encoded query parameter formatting, headers, and request body streaming.

## Error Translation Specification

All HTTP responses and transport events are mapped directly into the OmniSync structured error hierarchy:
1. **HTTP 401 / 403:** Converted to `AuthenticationException` / `AuthenticationError`.
2. **HTTP 429:** Converted to `RateLimitExceededException` / `RateLimitExceededError`, extracting and parsing the `Retry-After` header into a duration / seconds float.
3. **HTTP 5xx & 4xx:** Converted to `NetworkException` / `NetworkError` preserving the HTTP status code.
4. **Transport, DNS, and Timeout Failures:** Wrapped in `NetworkException` / `NetworkError`.
