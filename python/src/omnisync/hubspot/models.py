"""Domain models for HubSpot CRM objects."""

from dataclasses import dataclass


@dataclass(frozen=True)
class HubSpotContact:
    """Typed domain representation of a HubSpot CRM contact."""

    id: str
    email: str = ""
    first_name: str = ""
    last_name: str = ""
    company: str = ""
    phone: str = ""
    created_at: str = ""
    updated_at: str = ""
    archived: bool = False
