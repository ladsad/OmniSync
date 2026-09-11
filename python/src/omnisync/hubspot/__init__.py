"""OmniSync HubSpot connector module."""

from omnisync.hubspot.connector import HubSpotConnector
from omnisync.hubspot.models import HubSpotContact
from omnisync.hubspot.parser import (
    HubSpotSearchResult,
    parse_hubspot_contact_response,
    stream_hubspot_contacts,
)

__all__ = [
    "HubSpotConnector",
    "HubSpotContact",
    "HubSpotSearchResult",
    "parse_hubspot_contact_response",
    "stream_hubspot_contacts",
]
