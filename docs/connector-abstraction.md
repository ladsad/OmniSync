# Connector Abstraction

## Overview

The `Connector` abstraction establishes the core contract that all third-party SaaS integrations (such as Jira and HubSpot) implement. By adhering to a unified interface, data extraction pipelines interact with disparate APIs through identical lifecycle semantics: authentication, paginated page fetching, record extraction, and standardized error handling.

## Interface Contract

### `Connector<T>` / `Connector[T]`
- `authenticate()`: Delegates to configured `AuthStrategy` to validate credentials, perform handshakes, or refresh expired access tokens.
- `fetchPage(cursor)` / `fetch_page(cursor)`: Fetches a discrete batch of records using a cursor/offset token. Returns a `Page<T>` containing records and the next page pointer.
- `extractRecords(page)` / `extract_records(page)`: Extracts and normalizes domain items from the raw page structure.
- `handleError(error)` / `handle_error(error)`: Centralizes error classification, logging, or short-circuit policies.
- `paginate()`: Returns a lazy iterable/iterator streaming all records sequentially across pages.

## Base Class (`BaseConnector`)

The `BaseConnector` provides reusable default implementations:
- Injects and holds the `AuthStrategy`.
- Coordinates `paginate()` using `PaginationIterable` / `paginate()`.
- Implements defensive record extraction when pages are null or empty.
- Provides standard error propagation hooks ready for circuit breaker or retry layers.

## Implementation Guidelines for Future Connectors

When authoring a concrete connector (e.g. `JiraConnector`, `HubSpotConnector`):
1. **Auth Method:** Inject an `AuthStrategy` (`BasicAuthStrategy` or `OAuth2Strategy`) rather than embedding credentials in connector logic.
2. **Pagination Style:** Map provider pagination schemes (cursor, offset, page tokens) into the `Page<T>` record with `nextCursor`.
3. **Rate-Limit Behavior:** Intercept provider 429 responses and translate them into `RateLimitExceededException` / `RateLimitExceededError` with retry delay context.
4. **Known Limitations:** Document provider-specific limits, such as maximum batch size (e.g. Jira `maxResults` cap) or rate limit burst quotas.
