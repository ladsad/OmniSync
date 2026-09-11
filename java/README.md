# OmniSync Java Runtime

Enterprise Java 17+ implementation of the OmniSync SaaS integration framework.

---

## Build & Test

### Requirements
- Java Development Kit (JDK) 17 or newer (target: Java 17 LTS `--release 17`)
- Apache Maven 3.8+

### Commands

```bash
# Compile and run all 55 tests
mvn clean test

# Run tests and generate JaCoCo coverage report
mvn test jacoco:report
# View report at target/site/jacoco/index.html

# Run JSON streaming benchmark suite
mvn test -Dtest=JsonParsingBenchmarkTest
```

---

## Package Overview

- `com.omnisync.core.auth`: Authentication strategy pattern (`BasicAuthStrategy`, `OAuth2Strategy`).
- `com.omnisync.core.connector`: Base connector contract and abstract lifecycle.
- `com.omnisync.core.error`: Structured runtime exception hierarchy (`AuthenticationException`, `RateLimitExceededException`, `MalformedDataException`, `NetworkException`, `CircuitBreakerOpenException`).
- `com.omnisync.core.http`: Lightweight standard library `java.net.http.HttpClient` wrapper.
- `com.omnisync.core.json`: High-throughput token-level streaming parser (`JsonParsingPipeline`, `JsonStreamReader`).
- `com.omnisync.core.pagination`: Lazy memory-bounded stream iterator (`PaginationIterator`, `PaginationIterable`).
- `com.omnisync.core.resilience`: Exponential backoff with full jitter (`RetryPolicy`) and 3-state machine (`CircuitBreaker`, `ResilientHttpClient`).
- `com.omnisync.jira`: Jira issue connector, typed models, and streaming parser.
- `com.omnisync.hubspot`: HubSpot contact connector, typed models, and streaming parser.

---

## Usage Example

```java
import com.omnisync.core.auth.BasicAuthStrategy;
import com.omnisync.core.http.DefaultHttpClient;
import com.omnisync.core.resilience.CircuitBreaker;
import com.omnisync.core.resilience.ResilientHttpClient;
import com.omnisync.core.resilience.RetryPolicy;
import com.omnisync.jira.JiraConnector;
import com.omnisync.jira.model.JiraIssue;

import java.time.Duration;

var client = new ResilientHttpClient(
    new DefaultHttpClient(),
    new RetryPolicy(3, Duration.ofMillis(500), Duration.ofSeconds(10), 2.0),
    new CircuitBreaker(5, Duration.ofSeconds(30), 2)
);

var jira = new JiraConnector(
    "https://your-domain.atlassian.net",
    new BasicAuthStrategy("user@example.com", "token"),
    client
);

for (JiraIssue issue : jira.paginate(50)) {
    System.out.println(issue.key() + ": " + issue.summary());
}
```
