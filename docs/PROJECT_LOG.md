# OmniSync Project Log

This document records the chronological history of work completed, architectural and engineering decisions, challenges encountered, and their resolutions.

---

## Log Entries

### Entry: Project Initialization & Core Abstraction Scaffolding
- **Date:** 2026-09-11
- **Author:** OmniSync Team

#### Work Completed
1. **Multi-Runtime Repository Scaffolding:**
   - Initialized Java module under [`java/`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java) with [`java/pom.xml`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/pom.xml) targeting Java 17 LTS, equipped with JUnit 5, AssertJ, Mockito, and JaCoCo.
   - Initialized Python package under [`python/`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python) with [`python/pyproject.toml`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/pyproject.toml) and [`python/requirements.txt`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/requirements.txt) supporting Python 3.10+ and configured with PyTest and pytest-cov.
   - Configured root [`.gitignore`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/.gitignore) ignoring build outputs, virtual environments, and IDE artifacts.

2. **Core Abstractions Defined (Java & Python):**
   - **Structured Error Hierarchy:**
     - Java: [`OmniSyncException`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/core/error/OmniSyncException.java), [`AuthenticationException`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/core/error/AuthenticationException.java), [`RateLimitExceededException`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/core/error/RateLimitExceededException.java), [`MalformedDataException`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/core/error/MalformedDataException.java), [`NetworkException`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/core/error/NetworkException.java).
     - Python: [`OmniSyncError`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/error/exceptions.py), [`AuthenticationError`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/error/exceptions.py), [`RateLimitExceededError`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/error/exceptions.py), [`MalformedDataError`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/error/exceptions.py), [`NetworkError`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/error/exceptions.py).
   - **Authentication Strategy Pattern:**
     - Java: [`AuthStrategy`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/core/auth/AuthStrategy.java), [`BasicAuthStrategy`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/core/auth/BasicAuthStrategy.java), [`OAuth2Strategy`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/core/auth/OAuth2Strategy.java).
     - Python: [`AuthStrategy`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/auth/strategy.py), [`BasicAuthStrategy`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/auth/basic.py), [`OAuth2Strategy`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/auth/oauth2.py).
   - **Pagination Stream Abstractions:**
     - Java: [`Page`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/core/pagination/Page.java), [`PageFetcher`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/core/pagination/PageFetcher.java), [`PaginationIterator`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/core/pagination/PaginationIterator.java), [`PaginationIterable`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/core/pagination/PaginationIterable.java).
     - Python: [`Page`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/pagination/paginator.py), [`PaginationIterator`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/pagination/paginator.py), `paginate()`.
   - **Connector Contracts & Base Classes:**
     - Java: [`Connector`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/core/connector/Connector.java), [`BaseConnector`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/core/connector/BaseConnector.java).
     - Python: [`Connector`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/connector/base.py), [`BaseConnector`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/connector/base.py).

3. **Testing & Coverage:**
   - 19 JUnit 5 test cases passing across all Java abstractions.
   - 20 PyTest test cases passing with 100% statement coverage across all Python modules.

4. **Documentation:**
   - Published individual abstraction docs:
     - [`docs/error-hierarchy.md`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/docs/error-hierarchy.md)
     - [`docs/auth-strategy-abstraction.md`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/docs/auth-strategy-abstraction.md)
     - [`docs/pagination-abstraction.md`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/docs/pagination-abstraction.md)
     - [`docs/connector-abstraction.md`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/docs/connector-abstraction.md)

5. **Continuous Integration:**
   - Configured GitHub Actions workflow [`.github/workflows/ci.yml`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/.github/workflows/ci.yml) testing Java and Python matrices on push and pull requests.

#### Architectural & Engineering Decisions
- **Unchecked Exception Hierarchy:** Chose runtime exceptions (`RuntimeException` in Java and `Exception` in Python) to prevent checked-exception propagation pollution through Java streams and iterator lambdas.
- **Lazy Buffered Pagination:** Pagination fetcher uses an internal buffer populated on demand, yielding records one-by-one and querying subsequent pages only when the buffer drains. This bounds memory consumption regardless of dataset size.
- **Target Java 17 LTS via `--release 17`:** Ensures byte-code cross-compatibility across JDK 17+ runtimes while avoiding JDK module mismatch warnings on newer developer environments (such as JDK 25).
- **Strict Free-Tier Tooling:** All dependencies (JUnit 5, AssertJ, Mockito, JaCoCo, PyTest, GitHub Actions standard runners) are 100% open-source and free without commercial tiers or metering.

#### Problems Faced & Resolutions
- **Issue 1: JaCoCo Bytecode Instrumentation Failure on JDK 25**
  - *Symptom:* `java.lang.IllegalArgumentException: Unsupported class file major version 69` occurred during Surefire test execution when JaCoCo attempted to instrument JVM internal locale classes.
  - *Resolution:* Upgraded `jacoco-maven-plugin` to version `0.8.12` and added an explicit `<includes><include>com/omnisync/**</include></includes>` filter so JaCoCo instruments only application classes.
- **Issue 2: Maven Compiler Warning for System Modules**
  - *Symptom:* `location of system modules is not set in conjunction with -source 17`.
  - *Resolution:* Replaced `-source 17` and `-target 17` with `<release>17</release>` in `maven-compiler-plugin`.

---

### Entry: HTTP Transport Layer Implementation
- **Date:** 2026-09-11
- **Author:** OmniSync Team

#### Work Completed
1. **Request & Response Value Models:**
   - Java: Implemented [`HttpRequest`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/core/http/HttpRequest.java) with fluent builder, query parameter support, and [`HttpResponse`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/core/http/HttpResponse.java) with case-insensitive header lookup.
   - Python: Implemented frozen dataclasses [`HttpRequest`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/http/models.py) and [`HttpResponse`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/http/models.py).

2. **HTTP Client Abstraction & Default Client:**
   - Java: Created [`HttpClient`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/core/http/HttpClient.java) contract and [`DefaultHttpClient`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/core/http/DefaultHttpClient.java) powered by standard `java.net.http.HttpClient`.
   - Python: Created [`HttpClient`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/http/client.py) contract and [`DefaultHttpClient`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/http/client.py) powered by standard library `urllib.request`.

3. **Status Code Translation:**
   - Automatically maps HTTP 401/403 to `AuthenticationException` / `AuthenticationError`.
   - Automatically maps HTTP 429 to `RateLimitExceededException` / `RateLimitExceededError` with parsed `Retry-After` durations.
   - Automatically maps 5xx and 4xx to `NetworkException` / `NetworkError`.

4. **Integration Testing & Coverage:**
   - Java: Added 8 in-process HTTP tests using JDK `com.sun.net.httpserver.HttpServer` in [`HttpClientTest.java`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/test/java/com/omnisync/core/http/HttpClientTest.java) (total 27 passing tests).
   - Python: Added 7 in-process HTTP tests using `http.server.HTTPServer` in [`test_http.py`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/tests/test_http.py) (total 27 passing tests, 100% total codebase coverage).

5. **Documentation:**
   - Published [`docs/http-transport-layer.md`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/docs/http-transport-layer.md).

#### Architectural & Engineering Decisions
- **Zero Third-Party Network Dependencies:** Utilized standard library HTTP clients (`java.net.http.HttpClient` for Java 11+ and `urllib.request` for Python) ensuring 100% free, zero external binary attack surface, and no third-party version clashes.
- **In-Process Integration Test Strategy:** Emitted live HTTP requests against ephemeral local server instances (`com.sun.net.httpserver.HttpServer` and Python's `HTTPServer`) rather than mock wrappers. This thoroughly tests actual socket connections, headers, status codes, and timeouts.

#### Problems Faced & Resolutions
- **Issue 1: Mockito JDK 25 Module Class Mocking**
  - *Symptom:* Mockito threw `MockitoException: Could not modify all classes [interface java.lang.AutoCloseable, class java.net.http.HttpClient]` when trying to mock `java.net.http.HttpClient` on JDK 25.
  - *Resolution:* Replaced mock assertions with a lightweight in-process JDK `HttpServer`, validating real socket serialization and error handling without depending on bytecode manipulation.
- **Issue 2: Windows Winsock Connection Abort in Python Tests**
  - *Symptom:* `ConnectionAbortedError: [WinError 10053] An established connection was aborted by the software in your host machine` during HTTP method tests on Windows.
  - *Resolution:* Fixed test server request handler to explicitly consume all inbound payload bytes via `rfile.read(content_length)` and provide explicit `Content-Length` headers before closing sockets.

---

### Entry: Jira Connector Implementation
- **Date:** 2026-09-11
- **Author:** OmniSync Team

#### Work Completed
1. **Domain Model & Parsing:**
   - Java: Defined immutable [`JiraIssue`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/jira/model/JiraIssue.java) record and implemented [`JiraIssueParser`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/jira/parser/JiraIssueParser.java) backed by Jackson Databind.
   - Python: Defined frozen dataclass [`JiraIssue`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/jira/models.py) and implemented [`parse_jira_search_response()`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/jira/parser.py) using standard library `json`.
   - Built defensive parsing that extracts issue fields while skipping unidentifiable/corrupted items without halting batch ingestion.

2. **Jira Connector Implementation:**
   - Java: Implemented [`JiraConnector`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/jira/JiraConnector.java) extending `BaseConnector<JiraIssue>`.
   - Python: Implemented [`JiraConnector`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/jira/connector.py) extending `BaseConnector[JiraIssue]`.
   - Built seamless integration with `AuthStrategy` (Basic Auth with API tokens or OAuth2) and `HttpClient` transport.
   - Handled Jira `startAt` and `maxResults` query parameters and cursor calculations.

3. **Unit & Integration Testing:**
   - Java: 8 new tests across [`JiraIssueParserTest.java`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/test/java/com/omnisync/jira/parser/JiraIssueParserTest.java) and [`JiraConnectorTest.java`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/test/java/com/omnisync/jira/JiraConnectorTest.java) (total 35 passing tests in suite).
   - Python: 8 new tests across [`test_jira_parser.py`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/tests/test_jira_parser.py) and [`test_jira_connector.py`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/tests/test_jira_connector.py) (total 35 passing tests in suite, 100% total statement coverage).

4. **Documentation:**
   - Published [`docs/jira-connector.md`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/docs/jira-connector.md) documenting authentication methods, pagination style, rate-limit behavior, and known Jira limitations.

#### Architectural & Engineering Decisions
- **Jackson Databind Approval:** User approved adding `com.fasterxml.jackson.core:jackson-databind:2.17.0` (Apache 2.0 open-source) to Java to provide reliable typed parsing and high-throughput JSON tree navigation.
- **Defensive Item Skipping:** Individual malformed issue items within a batch are skipped rather than aborting the entire page fetch, maximizing pipeline resiliency for inconsistent SaaS payloads.
- **Uniform Pagination Interface:** Jira's numeric `startAt` offset was seamlessly adapted into OmniSync's opaque string cursor model, allowing callers to consume issues using standard `for` loops and streams.

#### Problems Faced & Resolutions
- **Issue 1: Non-Numeric Cursor Fallback**
  - *Symptom:* If an external caller passed an unexpected non-numeric cursor string to JiraConnector, `Integer.parseInt()` threw an unhandled `NumberFormatException`.
  - *Resolution:* Added defensive parsing in both Java and Python `fetchPage()` to fall back safely to `startAt=0` if a non-numeric cursor string is encountered.

---

### Entry: HubSpot Connector Implementation
- **Date:** 2026-09-11
- **Author:** OmniSync Team

#### Work Completed
1. **Domain Model & Parsing:**
   - Java: Implemented immutable [`HubSpotContact`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/hubspot/model/HubSpotContact.java) record, [`HubSpotSearchResult`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/hubspot/parser/HubSpotSearchResult.java), and [`HubSpotContactParser`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/hubspot/parser/HubSpotContactParser.java) using Jackson Databind.
   - Python: Implemented frozen dataclasses [`HubSpotContact`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/hubspot/models.py) and [`HubSpotSearchResult`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/hubspot/parser.py), alongside [`parse_hubspot_contact_response()`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/hubspot/parser.py).
   - Built defensive parsing handling missing contact IDs, properties dictionaries, and empty terminal paging markers.

2. **HubSpot Connector Implementation:**
   - Java: Created [`HubSpotConnector`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/hubspot/HubSpotConnector.java) extending `BaseConnector<HubSpotContact>`.
   - Python: Created [`HubSpotConnector`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/hubspot/connector.py) extending `BaseConnector[HubSpotContact]`.
   - Integrated cursor-based pagination using the `after` query parameter and `paging.next.after` cursor extraction.
   - Wired authentication header delegation via `AuthStrategy` (`OAuth2Strategy`).

3. **Unit & Integration Testing:**
   - Java: 7 new tests in [`HubSpotContactParserTest.java`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/test/java/com/omnisync/hubspot/parser/HubSpotContactParserTest.java) and [`HubSpotConnectorTest.java`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/test/java/com/omnisync/hubspot/HubSpotConnectorTest.java) (total 42 passing tests in suite).
   - Python: 7 new tests in [`test_hubspot_parser.py`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/tests/test_hubspot_parser.py) and [`test_hubspot_connector.py`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/tests/test_hubspot_connector.py) (total 42 passing tests in suite, 100% statement coverage).

4. **Documentation:**
   - Published [`docs/hubspot-connector.md`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/docs/hubspot-connector.md) documenting authentication methods, pagination style, rate-limit behavior, and known HubSpot API limitations.

#### Architectural & Engineering Decisions
- **Extensibility Validation (Open/Closed Principle):** Added HubSpot connector reusing the identical `BaseConnector`, `HttpClient`, and `PaginationIterator` infrastructure without modifying core pipeline abstractions.
- **Opaque Cursor Strategy:** HubSpot's string-based cursor token `after` maps directly to OmniSync's `Page.nextCursor`, proving the generality of the pagination abstraction across both offset-based (Jira) and cursor-based (HubSpot) APIs.

#### Problems Faced & Resolutions
- **Issue 1: Property Flattening in Response Payloads**
  - *Symptom:* HubSpot nests business attributes inside a `properties` sub-object while returning `createdAt` and `updatedAt` at the root and alternatively inside properties (`createdate`, `lastmodifieddate`).
  - *Resolution:* Implemented fallback extraction logic in both Java and Python parsers to inspect root keys and fall back to inner property aliases seamlessly.

---

### Entry: Resilience Layer (Exponential Backoff, Rate-Limiting, Circuit Breaker)
- **Date:** 2026-09-11
- **Author:** OmniSync Team

#### Work Completed
1. **Error Hierarchy Extension:**
   - Java: Added [`CircuitBreakerOpenException`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/core/error/CircuitBreakerOpenException.java) extending `OmniSyncException`.
   - Python: Added [`CircuitBreakerOpenError`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/error/exceptions.py) extending `OmniSyncError`.

2. **Retry Policy with Exponential Backoff & Jitter:**
   - Java: Implemented [`RetryPolicy`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/core/resilience/RetryPolicy.java) calculating exponential backoff with full jitter and respecting `RateLimitExceededException.getRetryAfter()`.
   - Python: Implemented [`RetryPolicy`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/resilience/retry.py) supporting exponential backoff, full jitter, and respecting `RateLimitExceededError.retry_after_seconds`.

3. **Circuit Breaker State Machine:**
   - Java: Implemented [`CircuitBreaker`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/core/resilience/CircuitBreaker.java) governing CLOSED, OPEN, and HALF_OPEN state transitions.
   - Python: Implemented [`CircuitBreaker`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/resilience/circuit_breaker.py) with identical transition mechanics.

4. **Resilient HTTP Client Decorator:**
   - Java: Implemented [`ResilientHttpClient`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/core/resilience/ResilientHttpClient.java) wrapping any `HttpClient` and accepting pluggable `Sleeper` for deterministic testing.
   - Python: Implemented [`ResilientHttpClient`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/resilience/client.py) with pluggable sleeper callable.

5. **Unit & Integration Testing:**
   - Java: 6 comprehensive tests in [`ResilienceTest.java`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/test/java/com/omnisync/core/resilience/ResilienceTest.java) (total 48 passing tests in suite).
   - Python: 7 comprehensive tests in [`test_resilience.py`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/tests/test_resilience.py) (total 49 passing tests in suite, 100% total statement coverage).

6. **Documentation:**
   - Published [`docs/resilience-layer.md`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/docs/resilience-layer.md).

#### Architectural & Engineering Decisions
- **Decorator Pattern for Transport Resilience:** Implemented `ResilientHttpClient` as a decorator conforming to the `HttpClient` interface. This ensures all existing and future connectors (`JiraConnector`, `HubSpotConnector`, etc.) gain full retry backoff and circuit breaking transparently without changing a single line of connector code.
- **Pluggable Sleepers for Deterministic Testing:** Created `Sleeper` functional interface in Java and injected `Callable[[float], None]` in Python to allow unit tests to mock sleep intervals without introducing slow real-time test thread sleeps.
- **Rate Limit Priority:** Explicit `Retry-After` durations returned by APIs on HTTP 429 take precedence over algorithmic exponential backoff, adhering strictly to upstream API quotas.

#### Problems Faced & Resolutions
- **Issue 1: Test Timing Flakiness on Windows Clocks**
  - *Symptom:* `test_circuit_breaker_lifecycle` in Python failed due to `recovery_timeout_seconds` lower bound clamp (`max(0.1, recovery_timeout_seconds)`) when a test passed `0.05s`.
  - *Resolution:* Adjusted lower bound clamping to `max(0.0, recovery_timeout_seconds)` so unit test suites can configure sub-decisecond recovery timeouts reliably.

---

### Entry: JSON Parsing Pipeline Optimization & Empirical Benchmarks
- **Date:** 2026-09-11
- **Author:** OmniSync Team

#### Work Completed
1. **Centralized Parsing Abstractions:**
   - Java: Created [`JsonParsingPipeline`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/core/json/JsonParsingPipeline.java), [`JsonStreamReader`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/core/json/JsonStreamReader.java), and [`ItemParser`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/core/json/ItemParser.java) in `com.omnisync.core.json`.
   - Python: Created [`JsonParsingPipeline`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/json/pipeline.py) supporting `parse_dict()`, `stream_array()`, and `parse_array()`.

2. **Connector Parser Optimization:**
   - Java: Enhanced [`JiraIssueParser`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/jira/parser/JiraIssueParser.java) and [`HubSpotContactParser`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/main/java/com/omnisync/hubspot/parser/HubSpotContactParser.java) with streaming token-level parsers (`JsonParser`), skipping unused enterprise payload fields in constant time and avoiding intermediate AST DOM allocations.
   - Python: Enhanced [`omnisync.jira.parser`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/jira/parser.py) and [`omnisync.hubspot.parser`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/hubspot/parser.py) with lazy item generators ([`stream_jira_issues`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/jira/parser.py) and [`stream_hubspot_contacts`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/src/omnisync/hubspot/parser.py)).

3. **Empirical Benchmark Suite:**
   - Java: Implemented [`JsonParsingBenchmarkTest`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/java/src/test/java/com/omnisync/core/benchmark/JsonParsingBenchmarkTest.java) contrasting DOM baseline (`readTree`) against streaming token parsing on 1,000 issues with extra enterprise fields. Achieved ~1.47x - 1.56x speedup (~32% - 36% latency reduction) and >70% reduction in GC memory allocations.
   - Python: Implemented [`test_json_benchmark.py`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/python/tests/test_json_benchmark.py) demonstrating lazy generator streaming achieving ~30.3% latency reduction and 47 MB/s throughput with $O(1)$ memory per record.

4. **Testing & Coverage:**
   - Java: 55/55 unit tests passing across all packages with full JaCoCo coverage.
   - Python: 57/57 unit tests passing with 100% statement coverage.

5. **Documentation:**
   - Published [`docs/json-parsing-optimization.md`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/docs/json-parsing-optimization.md).

#### Architectural & Engineering Decisions
- **Token-Level Streaming vs Full Tree Construction:** Rather than inflating complete Jackson `JsonNode` AST trees for SaaS responses, low-level streaming scans tokens sequentially. Unneeded complex structures (such as nested custom fields, changelogs, descriptions) are bypassed via `parser.skipChildren()`, drastically decreasing heap allocation pressure.
- **Generator Streaming in Python:** For large ingestion batches, yielding parsed dataclass items via Python generators prevents accumulating thousands of objects in memory simultaneously before downstream processing begins.

#### Problems Faced & Resolutions
- **Issue 1: Token Advancement in JsonParsingPipeline Tests**
  - *Symptom:* Jackson parser was positioned at `START_OBJECT` when testing field extractions, causing initial `currentName()` to return `null` and skip fields.
  - *Resolution:* Standardized initial token inspection to verify `START_OBJECT` before entering field iteration loops.
- **Issue 2: Error String Discrepancy in Python Tests**
  - *Symptom:* Centralized Python parser returned `"JSON response root must be a dictionary object"` while existing tests expected `"root must be a JSON object"`.
  - *Resolution:* Aligned error string across the centralized pipeline to `"JSON response root must be a JSON object"`.

---

### Entry: Connector Authoring Guide & Framework Completeness
- **Date:** 2026-09-11
- **Author:** OmniSync Team

#### Work Completed
1. **Connector Authoring Documentation:**
   - Authored comprehensive [`docs/connector-authoring-guide.md`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/docs/connector-authoring-guide.md) documenting the contract for adding new third-party integrations (e.g. Zendesk, Salesforce) in Java and Python.
   - Provided side-by-side walkthrough code examples covering immutable domain models, defensive streaming JSON parsing, auth strategy injection, pagination strategy mapping (offset, cursor, and page-number), and resilience wrapping.
   - Defined connector validation checklist and standardized per-connector documentation requirements.

2. **Project Specification Fulfillment:**
   - Completed all requirements and milestones specified in [`docs/Project-Description.md`](file:///C:/Users/shaur/Desktop/Projects/OmniSync/docs/Project-Description.md).
   - Java 17 test suite: 55 unit tests passing with full JaCoCo code coverage.
   - Python 3.10+ test suite: 57 unit tests passing with 100% statement coverage.



