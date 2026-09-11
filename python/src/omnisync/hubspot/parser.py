"""Defensive parser for HubSpot CRM contact API responses."""

from dataclasses import dataclass, field
import json
from typing import Any, Optional

from omnisync.error.exceptions import MalformedDataError
from omnisync.hubspot.models import HubSpotContact


@dataclass(frozen=True)
class HubSpotSearchResult:
    """Parsed container with contacts and cursor token."""

    results: list[HubSpotContact] = field(default_factory=list)
    next_after: Optional[str] = None
    has_more: bool = False


def parse_hubspot_contact_response(raw_json: str) -> HubSpotSearchResult:
    """Parse raw JSON response from HubSpot CRM endpoint defensively.

    Args:
        raw_json: Raw JSON payload string.

    Returns:
        HubSpotSearchResult containing contacts and next page cursor.

    Raises:
        MalformedDataError: If JSON syntax is invalid or root is not an object.
    """
    if not raw_json or not raw_json.strip():
        raise MalformedDataError("Empty or null HubSpot JSON payload", raw_payload=raw_json)

    try:
        data = json.loads(raw_json)
    except Exception as exc:
        raise MalformedDataError(
            f"Failed to parse HubSpot response: {exc}",
            raw_payload=raw_json,
        ) from exc

    if not isinstance(data, dict):
        raise MalformedDataError(
            "HubSpot response root must be a JSON object",
            raw_payload=raw_json,
        )

    contacts_raw = data.get("results", [])
    contacts: list[HubSpotContact] = []

    if isinstance(contacts_raw, list):
        for item in contacts_raw:
            contact = _parse_single_contact(item)
            if contact is not None:
                contacts.append(contact)

    paging = data.get("paging")
    next_obj = paging.get("next") if isinstance(paging, dict) else None
    next_after = next_obj.get("after") if isinstance(next_obj, dict) else None
    has_more = bool(next_after)

    return HubSpotSearchResult(
        results=contacts,
        next_after=str(next_after) if next_after else None,
        has_more=has_more,
    )


def _parse_single_contact(item: Any) -> Optional[HubSpotContact]:
    """Parse single contact dict safely, skipping if id is missing."""
    if not isinstance(item, dict):
        return None

    contact_id = item.get("id")
    if not contact_id:
        return None

    props = item.get("properties")
    if not isinstance(props, dict):
        props = {}

    email = str(props.get("email") or "")
    first_name = str(props.get("firstname") or "")
    last_name = str(props.get("lastname") or "")
    company = str(props.get("company") or "")
    phone = str(props.get("phone") or "")

    created_at = str(item.get("createdAt") or props.get("createdate") or "")
    updated_at = str(item.get("updatedAt") or props.get("lastmodifieddate") or "")
    archived = bool(item.get("archived", False))

    return HubSpotContact(
        id=str(contact_id),
        email=email,
        first_name=first_name,
        last_name=last_name,
        company=company,
        phone=phone,
        created_at=created_at,
        updated_at=updated_at,
        archived=archived,
    )
