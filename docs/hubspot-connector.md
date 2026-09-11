# HubSpot Connector

## Overview

The `HubSpotConnector` extracts CRM objects (such as Contacts) from HubSpot REST API v3 (`/crm/v3/objects/{objectType}`). It implements OmniSync's `Connector<HubSpotContact>` / `Connector[HubSpotContact]` contract, leveraging cursor-based pagination and OAuth 2.0 authentication.

## Connector Specification

### 1. Authentication Method
- **Supported Schemes:**
  - **OAuth 2.0 (Bearer):** Standard HubSpot app OAuth 2.0 bearer token using [`OAuth2Strategy`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/docs/auth-strategy-abstraction.md).
  - **Private App Access Tokens:** Static Bearer tokens injected via [`OAuth2Strategy`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/docs/auth-strategy-abstraction.md) or custom header bearer schemes.
- **Header Structure:** `Authorization: Bearer <access_token>`.

### 2. Pagination Style
- **Type:** Cursor-based pagination using HubSpot's `after` query parameter and `paging.next.after` response structure.
- **Cursor Mechanics:**
  - Initial request omits the `after` parameter.
  - Successive requests pass `queryParam("after", cursor)`.
  - Termination condition: `paging.next.after` is omitted or null in the response payload.
- **Batch Sizing:** Configurable via `pageSize` (defaults to 50, maximum supported by HubSpot CRM v3 is 100).

### 3. Rate-Limit Behavior
- HubSpot applies burst (100 requests per 10 seconds for standard tiers) and daily rate limits, returning HTTP 429 when limits are breached.
- The underlying `HttpClient` intercepts HTTP 429, extracts the `Retry-After` header value (in seconds), and throws `RateLimitExceededException` (Java) / `RateLimitExceededError` (Python).
- The retry and resilience layer detects the exception and pauses before attempting further page retrieval.

### 4. Known Limitations
- **Page Size Cap:** HubSpot limits page size (`limit`) to a maximum of 100 records per API call.
- **Cursor Lifetime:** Cursors returned in `paging.next.after` are opaque and should be consumed sequentially within an active extraction session.
- **Custom Properties:** By default, standard contact properties (`email`, `firstname`, `lastname`, `company`, `phone`) are fetched; custom CRM properties must be explicitly requested via the `properties` parameter.

## Domain Model: `HubSpotContact`

- `id`: Unique HubSpot contact ID string.
- `email`: Contact email address.
- `firstName`: Contact first name.
- `lastName`: Contact last name.
- `company`: Associated company name.
- `phone`: Primary phone number.
- `createdAt`: ISO 8601 creation timestamp.
- `updatedAt`: ISO 8601 last modified timestamp.
- `archived`: Boolean flag indicating if record is soft-deleted.
