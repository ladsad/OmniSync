# OmniSync

[![CI](https://github.com/omnisync/omnisync/actions/workflows/ci.yml/badge.svg)](https://github.com/omnisync/omnisync/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.org/)
[![Python 3.10+](https://img.shields.io/badge/Python-3.10%2B-blue.svg)](https://www.python.org/)
[![Coverage](https://img.shields.io/badge/Coverage-100%25-brightgreen.svg)]()

**OmniSync** is an enterprise-grade SaaS integration framework implemented across **Java 17** and **Python 3.10+**. It provides a unified, extensible architecture for connecting third-party APIs (including Jira and HubSpot) with automatic pagination, pluggable authentication, exponential backoff, circuit-breaking resilience, and high-throughput streaming JSON parsing.

---

## Architecture Overview

```
┌────────────────────────────────────────────────────────────────────────┐
│                              Client Code                               │
│       for (Record r : connector.paginate()) { process(r); }            │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          Connector Layer                               │
│  BaseConnector<T> ──> [JiraConnector, HubSpotConnector, Custom...]     │
└───────────────────┬───────────────────────────────┬────────────────────┘
                    │                               │
                    ▼                               ▼
┌──────────────────────────────────────┐  ┌──────────────────────────────┐
│        Resilient HTTP Layer          │  │    Defensive JSON Pipeline   │
│  - ResilientHttpClient               │  │  - JsonParsingPipeline       │
│  - Exponential Backoff & Jitter      │  │  - Token-level JsonParser    │
│  - CircuitBreaker State Machine      │  │  - Lazy Stream Generators    │
│  - Automatic 429 Retry-After         │  │  - Constant-time skipping    │
└───────────────────┬──────────────────┘  └──────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────┐
│           Auth Strategy              │
│  - BasicAuthStrategy                 │
│  - OAuth2Strategy (refresh renewal)  │
└──────────────────────────────────────┘
```

---

## Key Features

- **Dual-Runtime Consistency:** Identical architecture, abstractions, and design patterns across Java 17 LTS and Python 3.10+.
- **Unified Connector Contract:** Adding a new SaaS integration requires implementing a single connector class without altering core pipeline code (Open/Closed Principle).
- **Automated Pagination:** Seamlessly stream records one-by-one via iterables and generators across offset-based (`startAt`), cursor-based (`after`), or token-based pagination.
- **Enterprise Resilience:**
  - Configurable **Exponential Backoff with Full Jitter** (`RetryPolicy`).
  - Native adherence to HTTP 429 `Retry-After` headers.
  - Three-state **Circuit Breaker** (`CLOSED`, `OPEN`, `HALF_OPEN`) preventing cascading system failures.
- **High-Throughput JSON Parsing:**
  - Java: Jackson token-level streaming bypassing unneeded payload metadata, yielding **~1.5x speedup** and **>70% GC allocation reduction**.
  - Python: Generator streaming processing records with **$O(1)$ memory**.
- **100% Free & Open-Source:** Zero paid API dependencies, zero vendor lock-in, and zero metered cloud requirements.

---

## Quickstart: Java 17

### 1. Prerequisites & Build
- JDK 17+ (e.g., Eclipse Temurin, OpenJDK)
- Apache Maven 3.8+

```bash
cd java
mvn clean test
```

### 2. Using Jira Connector (Java)

```java
import com.omnisync.core.auth.BasicAuthStrategy;
import com.omnisync.core.http.DefaultHttpClient;
import com.omnisync.core.resilience.CircuitBreaker;
import com.omnisync.core.resilience.ResilientHttpClient;
import com.omnisync.core.resilience.RetryPolicy;
import com.omnisync.jira.JiraConnector;
import com.omnisync.jira.model.JiraIssue;

import java.time.Duration;

public class JiraExample {
    public static void main(String[] args) {
        // 1. Configure resilient HTTP client with retries and circuit breaker
        var resilientClient = new ResilientHttpClient(
            new DefaultHttpClient(),
            new RetryPolicy(3, Duration.ofMillis(500), Duration.ofSeconds(10), 2.0),
            new CircuitBreaker(5, Duration.ofSeconds(30), 2)
        );

        // 2. Instantiate connector with auth strategy
        var jira = new JiraConnector(
            "https://your-domain.atlassian.net",
            new BasicAuthStrategy("admin@example.com", "api_token_here"),
            resilientClient
        );

        // 3. Stream issues lazily across all pages
        for (JiraIssue issue : jira.paginate(50)) {
            System.out.printf("[%s] %s (Status: %s)%n", issue.key(), issue.summary(), issue.status());
        }
    }
}
```

### 3. Using HubSpot Connector (Java)

```java
import com.omnisync.core.auth.OAuth2Strategy;
import com.omnisync.core.http.DefaultHttpClient;
import com.omnisync.hubspot.HubSpotConnector;
import com.omnisync.hubspot.model.HubSpotContact;

public class HubSpotExample {
    public static void main(String[] args) {
        var hubspot = new HubSpotConnector(
            "https://api.hubapi.com",
            new OAuth2Strategy("access_token_here"),
            new DefaultHttpClient()
        );

        for (HubSpotContact contact : hubspot.paginate(100)) {
            System.out.printf("Contact: %s %s <%s>%n", 
                contact.firstName(), contact.lastName(), contact.email());
        }
    }
}
```

---

## Quickstart: Python 3.10+

### 1. Prerequisites & Setup
- Python 3.10+
- Virtual environment recommended

```bash
# Create and activate virtual environment
python -m venv .venv
source .venv/bin/activate  # On Windows: .venv\Scripts\Activate.ps1

# Install package and test dependencies
pip install -r python/requirements.txt
pip install -e python/

# Run test suite with coverage
python -m pytest python/tests --cov=omnisync --cov-report=term-missing
```

### 2. Using Jira Connector (Python)

```python
from omnisync.auth.basic import BasicAuthStrategy
from omnisync.http.client import DefaultHttpClient
from omnisync.jira.connector import JiraConnector
from omnisync.resilience.client import ResilientHttpClient
from omnisync.resilience.retry import RetryPolicy
from omnisync.resilience.circuit_breaker import CircuitBreaker

# 1. Configure resilient transport
resilient_client = ResilientHttpClient(
    underlying_client=DefaultHttpClient(),
    retry_policy=RetryPolicy(max_retries=3, initial_backoff_seconds=0.5),
    circuit_breaker=CircuitBreaker(failure_threshold=5, recovery_timeout_seconds=30.0),
)

# 2. Instantiate connector
jira = JiraConnector(
    base_url="https://your-domain.atlassian.net",
    auth_strategy=BasicAuthStrategy("admin@example.com", "api_token_here"),
    http_client=resilient_client,
)

# 3. Stream issues lazily across all pages
for issue in jira.paginate(page_size=50):
    print(f"[{issue.key}] {issue.summary} (Priority: {issue.priority})")
```

### 3. Using HubSpot Connector (Python)

```python
from omnisync.auth.oauth2 import OAuth2Strategy
from omnisync.http.client import DefaultHttpClient
from omnisync.hubspot.connector import HubSpotConnector

hubspot = HubSpotConnector(
    base_url="https://api.hubapi.com",
    auth_strategy=OAuth2Strategy(access_token="access_token_here"),
    http_client=DefaultHttpClient(),
)

for contact in hubspot.paginate(page_size=100):
    print(f"Contact: {contact.first_name} {contact.last_name} <{contact.email}>")
```

---

## Repository Structure

```
OmniSync/
├── .github/workflows/
│   └── ci.yml                     # Multi-runtime matrix CI (Java 17 + Python 3.10/3.11/3.12/3.13)
├── docs/                          # Comprehensive technical documentation
│   ├── PROJECT_LOG.md             # Chronological engineering & architectural decision log
│   ├── Project-Description.md     # Architectural requirements and specification
│   ├── connector-authoring-guide.md # Guide for creating new connectors (e.g. Zendesk, Salesforce)
│   ├── connector-abstraction.md   # BaseConnector and Connector contracts
│   ├── auth-strategy-abstraction.md # BasicAuthStrategy and OAuth2Strategy patterns
│   ├── pagination-abstraction.md  # Lazy pagination iterator and page fetchers
│   ├── http-transport-layer.md    # Standard library HTTP transport layer
│   ├── resilience-layer.md        # Retry backoff, circuit breaking, rate limits
│   ├── json-parsing-optimization.md # Token streaming and benchmark performance
│   ├── jira-connector.md          # Jira REST integration documentation
│   └── hubspot-connector.md       # HubSpot CRM REST integration documentation
├── java/                          # Java 17 LTS Maven module
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/omnisync/
│       │   ├── core/              # Abstractions (auth, connector, error, http, json, pagination, resilience)
│       │   ├── hubspot/           # HubSpot connector, domain models, and streaming parser
│       │   └── jira/              # Jira connector, domain models, and streaming parser
│       └── test/java/com/omnisync/ # 55 JUnit 5 unit and benchmark tests
└── python/                        # Python 3.10+ package
    ├── pyproject.toml
    ├── requirements.txt
    ├── src/omnisync/
    │   ├── auth/                  # Basic and OAuth2 strategy implementations
    │   ├── connector/             # BaseConnector and contracts
    │   ├── error/                 # Structured exception hierarchy
    │   ├── http/                  # Standard library urllib HTTP client
    │   ├── hubspot/               # HubSpot connector and models
    │   ├── jira/                  # Jira connector and models
    │   ├── json/                  # Centralized parsing pipeline
    │   ├── pagination/            # Lazy paginator generator
    │   └── resilience/            # Retry policy and circuit breaker
    └── tests/                     # 57 PyTest unit and benchmark tests
```

---

## Documentation Index

| Topic | Documentation Link |
|---|---|
| **Adding New Connectors** | [Connector Authoring Guide](docs/connector-authoring-guide.md) |
| **Resilience & Fault Tolerance** | [Resilience Layer](docs/resilience-layer.md) |
| **JSON Optimization & Benchmarks** | [JSON Parsing Pipeline](docs/json-parsing-optimization.md) |
| **Jira Connector** | [Jira Connector Guide](docs/jira-connector.md) |
| **HubSpot Connector** | [HubSpot Connector Guide](docs/hubspot-connector.md) |
| **HTTP Transport Layer** | [HTTP Transport Layer](docs/http-transport-layer.md) |
| **Pagination Engine** | [Pagination Abstraction](docs/pagination-abstraction.md) |
| **Authentication Strategy** | [Auth Strategy Abstraction](docs/auth-strategy-abstraction.md) |
| **Error Handling** | [Error Hierarchy](docs/error-hierarchy.md) |
| **Engineering Project Log** | [Project Log](docs/PROJECT_LOG.md) |

---

## Quality & Test Verification

Both runtimes are covered with extensive unit tests, mock servers, and empirical benchmark suites:
- **Java:** `mvn test` executes **55 tests** verifying auth headers, socket transports, pagination exhaustion, circuit-breaker state transitions, and Jackson streaming throughput.
- **Python:** `pytest` executes **57 tests** with **100% statement coverage** across all modules.
