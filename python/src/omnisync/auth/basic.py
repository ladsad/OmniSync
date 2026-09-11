"""HTTP Basic Authentication strategy."""

import base64
from typing import Optional

from omnisync.auth.strategy import AuthStrategy
from omnisync.error.exceptions import AuthenticationError


class BasicAuthStrategy(AuthStrategy):
    """Basic Authentication strategy generating RFC 7617 credentials."""

    def __init__(self, username: str, password: str) -> None:
        """Initialize BasicAuthStrategy.

        Args:
            username: User account identifier or email.
            password: Password or API token.
        """
        self.username = username
        self.password = password
        self._cached_header: Optional[str] = None

    def authenticate(self) -> None:
        """Validate credentials and build authorization header.

        Raises:
            AuthenticationError: When username or password is empty.
        """
        if not self.username or not self.password:
            raise AuthenticationError(
                "Username and password must not be empty for Basic Auth"
            )
        credentials = f"{self.username}:{self.password}"
        encoded = base64.b64encode(credentials.encode("utf-8")).decode("utf-8")
        self._cached_header = f"Basic {encoded}"

    def get_auth_headers(self) -> dict[str, str]:
        """Return Authorization header with Basic scheme."""
        if self._cached_header is None:
            self.authenticate()
        return {"Authorization": str(self._cached_header)}

    def is_expired(self) -> bool:
        """Basic credentials do not have automated expiry."""
        return False

    def refresh(self) -> None:
        """Re-validate and refresh cached Basic header."""
        self.authenticate()
