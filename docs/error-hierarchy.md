# Error and Exception Hierarchy

## Overview

OmniSync provides a structured, runtime-oriented error hierarchy across both Java and Python runtimes to categorize failure modes consistently during SaaS API extraction and authentication.

## Class Hierarchy

### Java (`com.omnisync.core.error`)
- `OmniSyncException` (extends `RuntimeException`)
  - `AuthenticationException`
  - `RateLimitExceededException`
  - `MalformedDataException`
  - `NetworkException`

### Python (`omnisync.error`)
- `OmniSyncError` (inherits from `Exception`)
  - `AuthenticationError`
  - `RateLimitExceededError`
  - `MalformedDataError`
  - `NetworkError`

## Exception Details

### 1. `AuthenticationException` / `AuthenticationError`
- **When raised:** Invalid credentials, missing API tokens, expired OAuth2 tokens that fail renewal, or HTTP 401/403 responses.
- **Attributes:** Detail message, optional underlying cause.

### 2. `RateLimitExceededException` / `RateLimitExceededError`
- **When raised:** External API rate limit thresholds are reached (typically HTTP 429).
- **Attributes:**
  - Java: `Optional<Duration> getRetryAfter()`
  - Python: `retry_after_seconds: Optional[float]`

### 3. `MalformedDataException` / `MalformedDataError`
- **When raised:** Response bodies cannot be parsed (corrupted JSON) or violate mandatory contract constraints.
- **Attributes:**
  - Java: `Optional<String> getRawPayload()`
  - Python: `raw_payload: Optional[str]`

### 4. `NetworkException` / `NetworkError`
- **When raised:** Transport-level communication failures, read/connect timeouts, DNS resolution issues, or upstream 5xx server errors.
- **Attributes:**
  - Java: `OptionalInt getStatusCode()`
  - Python: `status_code: Optional[int]`
