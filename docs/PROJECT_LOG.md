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
