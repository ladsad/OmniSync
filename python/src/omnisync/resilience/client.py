"""Resilient HTTP client decorator with retries and circuit breaker."""

import time
from typing import Callable, Optional

from omnisync.error.exceptions import OmniSyncError
from omnisync.http.client import HttpClient
from omnisync.http.models import HttpRequest, HttpResponse
from omnisync.resilience.circuit_breaker import CircuitBreaker
from omnisync.resilience.retry import RetryPolicy

Sleeper = Callable[[float], None]


class ResilientHttpClient(HttpClient):
    """HTTP client decorator providing automatic retries, backoff, and circuit breaking."""

    def __init__(
        self,
        underlying_client: HttpClient,
        retry_policy: Optional[RetryPolicy] = None,
        circuit_breaker: Optional[CircuitBreaker] = None,
        sleeper: Optional[Sleeper] = None,
    ) -> None:
        """Initialize ResilientHttpClient.

        Args:
            underlying_client: Base HttpClient performing network operations.
            retry_policy: Policy defining retry attempts and delays.
            circuit_breaker: Circuit breaker tracking failure states.
            sleeper: Callable used to sleep between retry attempts (defaults to time.sleep).

        Raises:
            ValueError: If underlying_client is None.
        """
        if underlying_client is None:
            raise ValueError("underlying_client must not be None")
        self.underlying_client = underlying_client
        self.retry_policy = retry_policy or RetryPolicy()
        self.circuit_breaker = circuit_breaker or CircuitBreaker()
        self.sleeper = sleeper or time.sleep

    def execute(self, request: HttpRequest) -> HttpResponse:
        """Execute request with circuit breaking and retry loops."""
        attempt = 0

        while True:
            self.circuit_breaker.allow_execution()

            try:
                response = self.underlying_client.execute(request)
                self.circuit_breaker.record_success()
                return response
            except OmniSyncError as error:
                self.circuit_breaker.record_failure(error)

                if self.retry_policy.should_retry(error, attempt):
                    delay = self.retry_policy.compute_delay(error, attempt)
                    attempt += 1
                    self.sleeper(delay)
                else:
                    raise error
