# Authentication Strategy Abstraction

## Overview

OmniSync uses the Strategy design pattern to decouple authentication schemes from connector data extraction logic. The `AuthStrategy` abstraction encapsulates credential validation, header generation, expiration checks, and token refreshes.

## Core Contract

### Methods
- `authenticate()`: Validates configured credentials or initializes tokens. Throws authentication errors if credentials are missing or invalid.
- `getAuthHeaders()` / `get_auth_headers()`: Produces HTTP headers (such as `Authorization: Basic ...` or `Authorization: Bearer ...`) required for authenticated requests.
- `isExpired()` / `is_expired()`: Determines whether credentials or access tokens have passed their expiration window.
- `refresh()`: Renews expired credentials or exchanges a refresh token for an active access token.

## Implementations

### 1. `BasicAuthStrategy`
- **Auth Method:** RFC 7617 HTTP Basic Authentication (`Authorization: Basic <base64(user:token)>`).
- **Use Cases:** Jira API tokens with username/email, basic auth endpoints.
- **Expiration Behavior:** Non-expiring static credential; `is_expired()` returns `false`.
- **Known Limitations:** Does not manage token revocation or multi-factor challenges directly; credentials must be rotated at the upstream provider.

### 2. `OAuth2Strategy`
- **Auth Method:** RFC 6749 OAuth 2.0 Bearer tokens (`Authorization: Bearer <access_token>`).
- **Use Cases:** HubSpot private app/OAuth2 flows, Jira OAuth 2.0 integrations.
- **Expiration Behavior:** Evaluates expiration timestamps; triggers automatic refresh if expired when fetching headers.
- **Token Management:** Manages `accessToken`, `refreshToken`, and UTC `expiresAt`.
- **Known Limitations:** Stub implementation handles token state and lifecycle checks; actual HTTP token exchange endpoints are wired via the HTTP client transport layer.
