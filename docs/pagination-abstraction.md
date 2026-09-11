# Pagination Abstraction

## Overview

External SaaS providers employ diverse pagination mechanics:
- Offset/limit (e.g. Jira `startAt` and `maxResults`)
- Cursor-based (e.g. HubSpot `after` tokens)
- Page token/link-header based

OmniSync unifies these mechanics behind a lazy iterator/generator contract (`Page<T>`, `PageFetcher<T>`, `PaginationIterator<T>`), allowing callers to consume records sequentially without manual page tracking.

## Core Models

### 1. `Page<T>`
- **Fields:**
  - `records`: Immutable list of extracted records in the current page batch.
  - `nextCursor` / `next_cursor`: Opaque token/cursor pointing to the subsequent page (null if last page).
  - `hasNext` / `has_next`: Boolean flag indicating if more pages follow.
- **Factory Helpers:**
  - `Page.empty()`: Empty terminal page.
  - `Page.lastPage(records)`: Terminal page with records and `hasNext=false`.

### 2. `PageFetcher<T>`
- Functional interface/callable taking an optional cursor string and returning a `Page<T>`.
- Injected into the iterator by connectors.

### 3. `PaginationIterator<T>` & `PaginationIterable<T>`
- Implements standard `Iterator<T>` (Java) and iterator protocol (Python).
- Fetches pages lazily on demand when the in-memory record buffer is exhausted.
- Terminates cleanly when `has_next` is false or the fetcher returns empty/null.

## Pagination Characteristics & Rate Limiting

- **Pagination Style:** Uniform cursor-driven abstraction wrapping offset, page-token, and cursor schemes.
- **Rate-Limit Behavior:** When an underlying `fetch()` call triggers a rate limit (HTTP 429), backoff and retries are executed at the fetcher/connector layer before yielding the next item.
- **Memory Profile:** Only holds one page batch in memory at a time, avoiding out-of-memory errors on large data exports.
- **Known Limitations:** Forward-only traversal; reverse pagination is not currently supported.
