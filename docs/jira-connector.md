# Jira Connector

## Overview

The `JiraConnector` extracts issues and associated project metadata from Atlassian Jira Cloud using the Jira REST API v3 search endpoint (`/rest/api/3/search`). It conforms to OmniSync's `Connector<JiraIssue>` / `Connector[JiraIssue]` interface.

## Connector Specification

### 1. Authentication Method
- **Supported Schemes:**
  - **Basic Auth (API Token):** Atlassian account email address paired with an Atlassian API token via [`BasicAuthStrategy`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/docs/auth-strategy-abstraction.md).
  - **OAuth 2.0 (3LO):** Atlassian Cloud 3-legged OAuth 2.0 Bearer tokens via [`OAuth2Strategy`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/docs/auth-strategy-abstraction.md).
- **Header Structure:** Injected automatically through `Authorization` request headers.

### 2. Pagination Style
- **Type:** 0-indexed offset/limit pagination using Jira's `startAt` and `maxResults` query parameters.
- **Cursor Mechanics:**
  - Initial request starts at `startAt=0`.
  - Next cursor string is the integer offset `startAt + records_fetched`.
  - Termination condition: `startAt + records_fetched >= total` or when zero issues are returned.
- **Batch Sizing:** Configurable via `pageSize` (defaults to 50, maximum recommended by Jira Cloud is 100).

### 3. Rate-Limit Behavior
- Jira returns HTTP 429 when API rate limit or concurrency caps are reached.
- The underlying `HttpClient` detects HTTP 429, extracts the `Retry-After` header value (in seconds), and raises `RateLimitExceededException` (Java) / `RateLimitExceededError` (Python).
- Upstream retry or backoff policies utilize the parsed retry delay before resuming extraction.

### 4. Known Limitations
- **Max Batch Limit:** Jira REST search endpoints hard-cap `maxResults` at 100 issues per request. Setting higher page sizes is ignored or rejected by Jira.
- **Deep Pagination:** Jira instances with >10,000 issues recommend search pagination optimization (such as narrowing `jql` windows or date filters) to prevent slow offset queries.
- **Atlassian Document Format (ADF):** In REST v3, rich text fields (such as `description`) are formatted in ADF JSON rather than plain text or markdown; this connector extracts standard text and summary fields.

## Domain Model: `JiraIssue`

- `id`: Internal Jira issue ID string.
- `key`: Human-readable issue key (e.g. `PROJ-123`).
- `summary`: Issue summary / title.
- `status`: Current workflow status name (e.g. `Open`, `In Progress`, `Done`).
- `issueType`: Issue type name (e.g. `Bug`, `Task`, `Story`).
- `priority`: Priority name (e.g. `High`, `Medium`, `Low`).
- `created`: ISO 8601 creation timestamp.
- `updated`: ISO 8601 last update timestamp.
- `assignee`: Display name of assigned user or `Unassigned`.
