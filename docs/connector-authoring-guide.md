# Connector Authoring Guide

This guide provides a standardized, step-by-step walkthrough for adding new SaaS integrations (e.g., Zendesk, Salesforce, Stripe, GitHub) to the OmniSync framework in both **Java 17** and **Python 3.10+**.

---

## Architectural Principles

OmniSync connectors adhere strictly to the **Open/Closed Principle**: new SaaS integrations are added by implementing standard interfaces without modifying core framework code.

```
┌────────────────────────────────────────────────────────────┐
│                       Client Code                          │
│     for (Record r : connector.paginate()) { ... }          │
└─────────────────────────────┬──────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────┐
│                    BaseConnector<T>                        │
│                                                            │
│   authenticate() ──> AuthStrategy (BasicAuth / OAuth2)     │
│                                                            │
│   fetchPage() ─────> ResilientHttpClient ──> Upstream API  │
│                                                            │
│   extractRecords() ─> JsonParsingPipeline ──> Typed Domain │
└────────────────────────────────────────────────────────────┘
```

Every connector consists of four cohesive components:
1. **Domain Model:** Immutable, typed record/dataclass representing the extracted resource.
2. **Defensive JSON Parser:** High-throughput streaming parser converting API responses into domain models while skipping corrupt items.
3. **Auth Strategy:** Decoupled authentication handler (`BasicAuthStrategy` or `OAuth2Strategy`).
4. **Connector Class:** Extension of `BaseConnector<T>` managing endpoint URLs, query parameters, and cursor pagination.

---

## Step-by-Step Implementation Guide

### Step 1: Define the Domain Model

Domain models must be strictly immutable and typed. Avoid passing generic maps or raw JSON nodes to consumers.

#### Java 17
Use Java records:
```java
package com.omnisync.zendesk.model;

public record ZendeskTicket(
    String id,
    String subject,
    String description,
    String status,
    String priority,
    String createdAt,
    String updatedAt
) {}
```

#### Python 3.10+
Use frozen dataclasses:
```python
from dataclasses import dataclass

@dataclass(frozen=True)
class ZendeskTicket:
    id: str
    subject: str
    description: str
    status: str
    priority: str
    created_at: str
    updated_at: str
```

---

### Step 2: Implement the Defensive JSON Parser

Parsers must adhere to the **Defensive Ingestion Rule**:
- Corrupt, incomplete, or unidentifiable items must be omitted or defaulted without failing the entire batch.
- Fatal syntax errors or invalid root structures must raise `MalformedDataException` (Java) or `MalformedDataError` (Python).
- Use [`JsonParsingPipeline`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/docs/json-parsing-optimization.md) to maximize throughput and minimize memory allocations.

#### Java 17
```java
package com.omnisync.zendesk.parser;

import com.fasterxml.jackson.core.JsonToken;
import com.omnisync.core.error.MalformedDataException;
import com.omnisync.core.json.JsonParsingPipeline;
import com.omnisync.zendesk.model.ZendeskTicket;

import java.util.ArrayList;
import java.util.List;

public class ZendeskTicketParser {

    private final JsonParsingPipeline pipeline = new JsonParsingPipeline();

    public ZendeskSearchResult parseTickets(String rawJson) {
        return pipeline.parseString(rawJson, parser -> {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                throw new MalformedDataException("Root must be a JSON object", rawJson);
            }

            List<ZendeskTicket> tickets = new ArrayList<>();
            String nextPage = null;

            while (parser.nextToken() != JsonToken.END_OBJECT && parser.currentToken() != null) {
                String field = parser.currentName();
                if ("tickets".equals(field)) {
                    if (parser.nextToken() == JsonToken.START_ARRAY) {
                        while (parser.nextToken() != JsonToken.END_ARRAY && parser.currentToken() != null) {
                            if (parser.currentToken() == JsonToken.START_OBJECT) {
                                ZendeskTicket ticket = parseSingleTicket(parser);
                                if (ticket != null) {
                                    tickets.add(ticket);
                                }
                            } else {
                                parser.skipChildren();
                            }
                        }
                    }
                } else if ("next_page".equals(field)) {
                    nextPage = parser.nextTextValue();
                } else {
                    parser.nextToken();
                    parser.skipChildren();
                }
            }

            return new ZendeskSearchResult(tickets, nextPage);
        });
    }

    private ZendeskTicket parseSingleTicket(com.fasterxml.jackson.core.JsonParser parser) throws Exception {
        String id = null;
        String subject = "";
        String description = "";
        String status = "new";
        String priority = "normal";
        String createdAt = "";
        String updatedAt = "";

        while (parser.nextToken() != JsonToken.END_OBJECT && parser.currentToken() != null) {
            String field = parser.currentName();
            if ("id".equals(field)) {
                id = parser.nextTextValue();
            } else if ("subject".equals(field)) {
                subject = parser.nextTextValue();
            } else if ("description".equals(field)) {
                description = parser.nextTextValue();
            } else if ("status".equals(field)) {
                status = parser.nextTextValue();
            } else if ("priority".equals(field)) {
                priority = parser.nextTextValue();
            } else if ("created_at".equals(field)) {
                createdAt = parser.nextTextValue();
            } else if ("updated_at".equals(field)) {
                updatedAt = parser.nextTextValue();
            } else {
                parser.nextToken();
                parser.skipChildren();
            }
        }

        if (id == null || id.isBlank()) {
            return null; // Defensive skip
        }

        return new ZendeskTicket(id, subject, description, status, priority, createdAt, updatedAt);
    }
}
```

#### Python 3.10+
```python
from dataclasses import dataclass, field
from typing import Any, Optional
from omnisync.json.pipeline import JsonParsingPipeline
from omnisync.zendesk.models import ZendeskTicket

_PIPELINE = JsonParsingPipeline()

@dataclass(frozen=True)
class ZendeskSearchResult:
    tickets: list[ZendeskTicket] = field(default_factory=list)
    next_page: Optional[str] = None

def _parse_single_ticket(item: Any) -> Optional[ZendeskTicket]:
    if not isinstance(item, dict):
        return None
    ticket_id = item.get("id")
    if not ticket_id:
        return None
    return ZendeskTicket(
        id=str(ticket_id),
        subject=str(item.get("subject") or ""),
        description=str(item.get("description") or ""),
        status=str(item.get("status") or "new"),
        priority=str(item.get("priority") or "normal"),
        created_at=str(item.get("created_at") or ""),
        updated_at=str(item.get("updated_at") or ""),
    )

def parse_zendesk_tickets(raw_json: str) -> ZendeskSearchResult:
    data = _PIPELINE.parse_dict(raw_json)
    tickets = _PIPELINE.parse_array(raw_json, "tickets", _parse_single_ticket)
    next_page = data.get("next_page")
    return ZendeskSearchResult(
        tickets=tickets,
        next_page=str(next_page) if next_page else None,
    )
```

---

### Step 3: Implement the Connector Class

Extend [`BaseConnector<T>`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/docs/connector-abstraction.md) and implement:
- `getId()`: Unique identifier string for logging and circuit breaker isolation.
- `fetchPage(String cursor, int pageSize)`: Encapsulates URL formatting, query parameter translation, and response parsing.
- `extractRecords(Page<T> page)`: Returns the page's item list.

#### Pagination Strategies Mapping
- **Offset/Limit (e.g. Jira):** Convert opaque string `cursor` to integer offset:
  `int offset = cursor != null ? Integer.parseInt(cursor) : 0;`
  Next cursor: `String.valueOf(offset + records.size());`
- **Cursor/Token (e.g. HubSpot):** Forward the raw opaque string token directly:
  `builder.queryParam("after", cursor);`
  Next cursor: `result.nextAfter();`
- **Page Number (e.g. Zendesk):** Increment numeric page index:
  `builder.queryParam("page", cursor != null ? cursor : "1");`

#### Java 17 Implementation
```java
package com.omnisync.zendesk;

import com.omnisync.core.auth.AuthStrategy;
import com.omnisync.core.connector.BaseConnector;
import com.omnisync.core.http.HttpClient;
import com.omnisync.core.http.HttpRequest;
import com.omnisync.core.http.HttpResponse;
import com.omnisync.core.pagination.Page;
import com.omnisync.zendesk.model.ZendeskTicket;
import com.omnisync.zendesk.parser.ZendeskSearchResult;
import com.omnisync.zendesk.parser.ZendeskTicketParser;

import java.util.List;

public class ZendeskConnector extends BaseConnector<ZendeskTicket> {

    private final String baseUrl;
    private final ZendeskTicketParser parser;

    public ZendeskConnector(String baseUrl, AuthStrategy authStrategy, HttpClient httpClient) {
        super(authStrategy, httpClient);
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.parser = new ZendeskTicketParser();
    }

    @Override
    public String getId() {
        return "zendesk";
    }

    @Override
    public Page<ZendeskTicket> fetchPage(String cursor, int pageSize) {
        HttpRequest.Builder req = HttpRequest.builder()
                .url(baseUrl + "/api/v2/tickets.json")
                .queryParam("per_page", String.valueOf(pageSize));

        if (cursor != null && !cursor.isBlank()) {
            req.queryParam("page", cursor);
        }

        HttpResponse response = executeAuthenticated(req.build());
        ZendeskSearchResult result = parser.parseTickets(response.body());

        String nextCursor = null;
        if (result.nextPage() != null) {
            // Extract page query param from next_page URL or increment index
            nextCursor = extractPageParam(result.nextPage());
        }

        return new Page<>(result.tickets(), nextCursor, nextCursor != null);
    }

    @Override
    public List<ZendeskTicket> extractRecords(Page<ZendeskTicket> page) {
        return page.records();
    }

    private String extractPageParam(String url) {
        // extract page query parameter value
        return ...;
    }
}
```

#### Python 3.10+ Implementation
```python
from typing import Optional
from urllib.parse import parse_qs, urlparse

from omnisync.auth.strategy import AuthStrategy
from omnisync.connector.base import BaseConnector
from omnisync.http.client import HttpClient
from omnisync.http.models import HttpRequest
from omnisync.pagination.paginator import Page
from omnisync.zendesk.models import ZendeskTicket
from omnisync.zendesk.parser import parse_zendesk_tickets

class ZendeskConnector(BaseConnector[ZendeskTicket]):

    def __init__(
        self,
        base_url: str,
        auth_strategy: AuthStrategy,
        http_client: HttpClient,
    ) -> None:
        super().__init__(auth_strategy, http_client)
        self.base_url = base_url.rstrip("/")

    @property
    def connector_id(self) -> str:
        return "zendesk"

    def fetch_page(self, cursor: Optional[str] = None, page_size: int = 50) -> Page[ZendeskTicket]:
        params = {"per_page": str(page_size)}
        if cursor:
            params["page"] = cursor

        req = HttpRequest(
            url=f"{self.base_url}/api/v2/tickets.json",
            query_params=params,
        )
        response = self.execute_authenticated(req)
        result = parse_zendesk_tickets(response.body)

        next_cursor = None
        if result.next_page:
            parsed_url = urlparse(result.next_page)
            qs = parse_qs(parsed_url.query)
            pages = qs.get("page")
            if pages:
                next_cursor = pages[0]

        return Page(
            records=result.tickets,
            next_cursor=next_cursor,
            has_more=bool(next_cursor),
        )

    def extract_records(self, page: Page[ZendeskTicket]) -> list[ZendeskTicket]:
        return page.records
```

---

### Step 4: Add Transport Resilience

Always wrap the base `HttpClient` with [`ResilientHttpClient`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/docs/resilience-layer.md). This guarantees that rate-limiting (HTTP 429), exponential backoff, and circuit-breaking protect against cascading network failures automatically.

```java
HttpClient resilientClient = new ResilientHttpClient(
    new DefaultHttpClient(),
    new RetryPolicy(3, Duration.ofMillis(500), Duration.ofSeconds(10), 2.0),
    new CircuitBreaker(5, Duration.ofSeconds(30), 2)
);

ZendeskConnector connector = new ZendeskConnector(
    "https://company.zendesk.com",
    new BasicAuthStrategy("user@company.com", "api_token"),
    resilientClient
);

// Stream records lazily across all pages
for (ZendeskTicket ticket : connector.paginate(100)) {
    process(ticket);
}
```

---

## Required Connector Documentation

Every connector PR must include a documentation file in `docs/<connector-name>-connector.md` covering:

1. **Authentication:** Supported auth methods (`BasicAuthStrategy`, `OAuth2Strategy`), header schemes, required scopes.
2. **Pagination Style:** Cursor-based, offset-based, or link-header-based, with default and maximum batch sizes.
3. **Rate Limit Handling:** Observed rate limit HTTP status codes (429), reset headers (`Retry-After`, `X-RateLimit-Reset`), and backoff defaults.
4. **Known Limitations:** Upstream quota limits, unsupported data types, or eventual consistency caveats.

---

## Connector Authoring Checklist

- [ ] **Zero Cost Dependency:** No paid third-party libraries introduced.
- [ ] **Type Safety:** Immutable domain model (Java `record`, Python `@dataclass(frozen=True)`).
- [ ] **Defensive Parsing:** Missing non-essential fields default cleanly; unidentifiable items are skipped; fatal syntax errors raise `MalformedDataException` / `MalformedDataError`.
- [ ] **Pluggable Auth:** Injected via `AuthStrategy` without credentials embedded in connector logic.
- [ ] **Resilience:** Compatible with `ResilientHttpClient` decorator.
- [ ] **Test Coverage:** Unit tests achieve **95%+ coverage** covering valid parsing, corrupt data skipping, pagination boundary conditions, and mock HTTP responses.
- [ ] **Documentation:** `docs/<connector-name>-connector.md` and `docs/PROJECT_LOG.md` updated in the same commit.
