"""OAuth2 Bearer token authentication strategy."""

from datetime import datetime, timezone
from typing import Optional

from omnisync.auth.strategy import AuthStrategy
from omnisync.error.exceptions import AuthenticationError


class OAuth2Strategy(AuthStrategy):
    """OAuth2 authentication strategy managing Bearer token state."""

    def __init__(
        self,
        client_id: str,
        client_secret: str,
        token_endpoint: str,
    ) -> None:
        """Initialize OAuth2Strategy.

        Args:
            client_id: OAuth2 client identifier.
            client_secret: OAuth2 client secret.
            token_endpoint: URL for token exchanges and refreshes.
        """
        self.client_id = client_id
        self.client_secret = client_secret
        self.token_endpoint = token_endpoint
        self.access_token: Optional[str] = None
        self.refresh_token: Optional[str] = None
        self.expires_at: Optional[datetime] = None

    def set_tokens(
        self,
        access_token: str,
        refresh_token: str,
        expires_at: Optional[datetime] = None,
    ) -> None:
        """Store active token pair and optional expiration timestamp.

        Args:
            access_token: Bearer access token string.
            refresh_token: Token string used to refresh access.
            expires_at: Expiration timestamp in UTC.
        """
        self.access_token = access_token
        self.refresh_token = refresh_token
        self.expires_at = expires_at

    def is_expired(self) -> bool:
        """Check if access token has passed its expiration timestamp."""
        if self.expires_at is None:
            return False
        return datetime.now(timezone.utc) > self.expires_at

    def authenticate(self) -> None:
        """Ensure active token is valid or perform refresh.

        Raises:
            AuthenticationError: When client settings or tokens are invalid.
        """
        if not self.client_id or not self.token_endpoint:
            raise AuthenticationError(
                "OAuth2 configuration requires valid client_id and token_endpoint"
            )
        if self.access_token is None:
            if self.refresh_token is not None:
                self.refresh()
            else:
                raise AuthenticationError(
                    "No OAuth2 access token or refresh token available"
                )
        elif self.is_expired():
            self.refresh()

    def get_auth_headers(self) -> dict[str, str]:
        """Return Authorization header with Bearer scheme."""
        if self.access_token is None or self.is_expired():
            self.authenticate()
        return {"Authorization": f"Bearer {self.access_token}"}

    def refresh(self) -> None:
        """Refresh access token using active refresh token.

        Raises:
            AuthenticationError: If refresh token is missing.
        """
        if not self.refresh_token:
            raise AuthenticationError(
                "Cannot refresh OAuth2 token: refresh token is absent"
            )
        # Token exchange stub: simulates token renewal
        self.access_token = f"refreshed_{self.refresh_token}"
        self.expires_at = datetime.now(timezone.utc).replace(year=datetime.now(timezone.utc).year + 1)
