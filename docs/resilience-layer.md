# OmniSync Resilience Layer

The OmniSync resilience layer provides enterprise-grade fault tolerance, transient error mitigation, and cascading failure protection for external SaaS API integrations in both Java and Python.

---

## Architectural Overview

Third-party SaaS APIs inevitably experience transient disruptions, server-side throttling (HTTP 429), and network timeouts. OmniSync safeguards ingestion pipelines by combining two coordinated resilience primitives into a composable HTTP client decorator (`ResilientHttpClient`):

1. **Exponential Backoff with Full Jitter Retry (`RetryPolicy`):** Retries transient network failures and 5xx errors with increasing delays, respecting explicit `Retry-After` headers when available, and adding randomized jitter to avoid thundering herd problems.
2. **Circuit Breaker State Machine (`CircuitBreaker`):** Monitors failure rates and fast-fails upstream requests when target services are down, giving degraded upstream providers time to recover before cautiously probing them.

```
Incoming Request
       │
       ▼
┌─────────────────────────────────────────────────────────────┐
│                   ResilientHttpClient                       │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐   │
│   │               CircuitBreaker Guard                  │   │
│   │                                                     │   │
│   │   [CLOSED] ──(failures >= threshold)──> [OPEN]      │   │
│   │      ▲                                     │        │   │
│   │      │ (successes >= threshold)            │        │   │
│   │      │                               (timeout elapses)  │
│   │      │                                     │        │   │
│   │   [HALF_OPEN] <────────────────────────────┘        │   │
│   └──────────────────────────┬──────────────────────────┘   │
│                              │ allows                       │
│                              ▼                              │
│   ┌─────────────────────────────────────────────────────┐   │
│   │                    Retry Loop                       │   │
│   │                                                     │   │
│   │   Execute Request ──> Catch Transient Failure       │   │
│   │          │                     │                    │   │
│   │          │ (success)     (evaluate retry)           │   │
│   │          ▼                     │                    │   │
│   │   Record Success         Backoff Sleep & Retry      │   │
│   └─────────────────────────────────────────────────────┘   │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
                      Underlying HttpClient
```

---

## Core Components

### 1. Retry Policy (`RetryPolicy`)

Governs retry eligibility and calculates dynamic backoff intervals.

- **Transient Error Classification:**
  - Network connectivity issues, timeouts, socket closures (`NetworkException` / `NetworkError`).
  - Server errors (HTTP 500, 502, 503, 504).
  - Rate limiting (HTTP 429 `RateLimitExceededException` / `RateLimitExceededError`).
  - *Non-transient errors (HTTP 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found, Malformed Data) are never retried.*
- **Delay Computation:**
  - If the failure is a rate limit error with an explicit `retryAfterSeconds`, OmniSync respects that exact duration.
  - Otherwise, exponential backoff is calculated:
    $$\text{backoff} = \min(\text{maxBackoff}, \text{initialBackoff} \times 2^{\text{attempt}})$$
  - Full jitter is applied:
    $$\text{delay} = \text{random}(0, \text{backoff})$$

### 2. Circuit Breaker (`CircuitBreaker`)

Implements the classic three-state automaton to prevent cascading failures:

- **CLOSED (Normal Operation):** Requests pass directly to the underlying client. Consecutive transient failures increment the internal failure counter. If failures reach `failureThreshold` (default: 5), the circuit trips to `OPEN`.
- **OPEN (Failing Fast):** Requests are rejected immediately without hitting the network by throwing `CircuitBreakerOpenException` (Java) or `CircuitBreakerOpenError` (Python). Once `recoveryTimeout` (default: 30s) elapses, the next request transitions the circuit to `HALF_OPEN`.
- **HALF_OPEN (Trial / Probing):** A limited number of trial requests are permitted. Any single transient failure immediately returns the circuit to `OPEN`. If `successThreshold` (default: 2) consecutive requests succeed, the circuit resets to `CLOSED`.

### 3. Resilient HTTP Client (`ResilientHttpClient`)

A transparent decorator wrapping any `HttpClient` instance. Implements the identical `HttpClient` interface, enabling drop-in replacement across all connectors (`JiraConnector`, `HubSpotConnector`, etc.).

---

## Usage Examples

### Java 17

```java
import com.omnisync.core.http.DefaultHttpClient;
import com.omnisync.core.http.HttpClient;
import com.omnisync.core.http.HttpRequest;
import com.omnisync.core.http.HttpResponse;
import com.omnisync.core.resilience.CircuitBreaker;
import com.omnisync.core.resilience.ResilientHttpClient;
import com.omnisync.core.resilience.RetryPolicy;

import java.time.Duration;

// 1. Configure policies
RetryPolicy retryPolicy = new RetryPolicy(
    3,                       // max retries
    Duration.ofMillis(500),  // initial backoff
    Duration.ofSeconds(10),  // max backoff
    2.0                      // backoff multiplier
);

CircuitBreaker circuitBreaker = new CircuitBreaker(
    5,                       // failure threshold
    Duration.ofSeconds(30),  // recovery timeout
    2                        // success threshold in half-open
);

// 2. Decorate base HTTP client
HttpClient baseClient = new DefaultHttpClient();
HttpClient resilientClient = new ResilientHttpClient(baseClient, retryPolicy, circuitBreaker);

// 3. Execute requests with automatic retries and circuit breaking
HttpRequest request = HttpRequest.builder()
    .url("https://api.atlassian.com/ex/jira/search")
    .build();

HttpResponse response = resilientClient.execute(request);
```

### Python 3.10+

```python
from omnisync.http.client import DefaultHttpClient
from omnisync.http.models import HttpRequest
from omnisync.resilience.circuit_breaker import CircuitBreaker
from omnisync.resilience.client import ResilientHttpClient
from omnisync.resilience.retry import RetryPolicy

# 1. Configure policies
retry_policy = RetryPolicy(
    max_retries=3,
    initial_backoff_seconds=0.5,
    max_backoff_seconds=10.0,
    backoff_multiplier=2.0,
)

circuit_breaker = CircuitBreaker(
    failure_threshold=5,
    recovery_timeout_seconds=30.0,
    success_threshold=2,
)

# 2. Decorate base HTTP client
base_client = DefaultHttpClient()
resilient_client = ResilientHttpClient(
    underlying_client=base_client,
    retry_policy=retry_policy,
    circuit_breaker=circuit_breaker,
)

# 3. Execute requests with automatic retries and circuit breaking
request = HttpRequest(url="https://api.hubspot.com/crm/v3/objects/contacts")
response = resilient_client.execute(request)
```

---

## Test Verification

- **Java (`ResilienceTest.java`):** 6 tests validating exponential backoff, retry exhaustion, non-retryable bypass, rate-limit header adherence, full circuit breaker state transitions, and half-open failure recovery.
- **Python (`test_resilience.py`):** 7 tests with 100% statement coverage validating backoff delay calculations, rate limit overrides, fast-fail circuit breaker behavior, and resilient decorator end-to-end execution.
