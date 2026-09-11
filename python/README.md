# OmniSync Python Runtime

Enterprise Python 3.10+ implementation of the OmniSync SaaS integration framework.

---

## Setup & Testing

### Requirements
- Python 3.10 or newer (tested on 3.10, 3.11, 3.12, 3.13)
- `pip`

### Commands

```bash
# Set up virtual environment
python -m venv .venv
source .venv/bin/activate  # On Windows: .venv\Scripts\Activate.ps1

# Install package in editable mode and test dependencies
pip install -r requirements.txt
pip install -e .

# Run all 57 tests with statement coverage report
python -m pytest tests --cov=omnisync --cov-report=term-missing

# Run JSON streaming benchmark test
python -m pytest tests/test_json_benchmark.py -s
```

---

## Module Overview

- `omnisync.auth`: Pluggable authentication strategies (`BasicAuthStrategy`, `OAuth2Strategy`).
- `omnisync.connector`: Base connector contract and abstract lifecycle (`BaseConnector`).
- `omnisync.error`: Structured exception hierarchy (`AuthenticationError`, `RateLimitExceededError`, `MalformedDataError`, `NetworkError`, `CircuitBreakerOpenError`).
- `omnisync.http`: Lightweight standard library `urllib.request` transport layer.
- `omnisync.json`: High-throughput parsing pipeline and array stream generator (`JsonParsingPipeline`).
- `omnisync.pagination`: Lazy memory-bounded page stream generator (`paginate`).
- `omnisync.resilience`: Exponential backoff with full jitter (`RetryPolicy`) and 3-state machine (`CircuitBreaker`, `ResilientHttpClient`).
- `omnisync.jira`: Jira issue connector, domain models, and streaming parser.
- `omnisync.hubspot`: HubSpot contact connector, domain models, and streaming parser.

---

## Usage Example

```python
from omnisync.auth.basic import BasicAuthStrategy
from omnisync.http.client import DefaultHttpClient
from omnisync.jira.connector import JiraConnector
from omnisync.resilience.client import ResilientHttpClient
from omnisync.resilience.retry import RetryPolicy
from omnisync.resilience.circuit_breaker import CircuitBreaker

client = ResilientHttpClient(
    underlying_client=DefaultHttpClient(),
    retry_policy=RetryPolicy(max_retries=3, initial_backoff_seconds=0.5),
    circuit_breaker=CircuitBreaker(failure_threshold=5, recovery_timeout_seconds=30.0),
)

jira = JiraConnector(
    base_url="https://your-domain.atlassian.net",
    auth_strategy=BasicAuthStrategy("user@example.com", "token"),
    http_client=client,
)

for issue in jira.paginate(page_size=50):
    print(f"{issue.key}: {issue.summary}")
```
