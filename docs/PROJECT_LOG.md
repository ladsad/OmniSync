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
