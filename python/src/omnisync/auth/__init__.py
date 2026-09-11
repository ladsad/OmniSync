"""OmniSync authentication strategies package."""

from omnisync.auth.basic import BasicAuthStrategy
from omnisync.auth.oauth2 import OAuth2Strategy
from omnisync.auth.strategy import AuthStrategy

__all__ = [
    "AuthStrategy",
    "BasicAuthStrategy",
    "OAuth2Strategy",
]
