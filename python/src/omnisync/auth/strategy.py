"""Base authentication strategy contract for OmniSync."""

from abc import ABC, abstractmethod


class AuthStrategy(ABC):
    """Abstract strategy defining authentication and token header provisioning."""

    @abstractmethod
    def authenticate(self) -> None:
        """Authenticate with the upstream service or validate existing credentials.

        Raises:
            AuthenticationError: If credentials or tokens are invalid/missing.
        """

    @abstractmethod
    def get_auth_headers(self) -> dict[str, str]:
        """Return HTTP authorization headers to attach to outbound requests.

        Returns:
            Dictionary containing authorization header key-value pairs.
        """

    @abstractmethod
    def is_expired(self) -> bool:
        """Indicate whether active credentials or access tokens are expired.

        Returns:
            True if credentials require refresh, False otherwise.
        """

    @abstractmethod
    def refresh(self) -> None:
        """Refresh expired credentials or OAuth2 tokens.

        Raises:
            AuthenticationError: If refreshing fails or refresh token is absent.
        """
