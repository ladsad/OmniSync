"""Defensive parser for HubSpot CRM contact API responses."""

from dataclasses import dataclass, field
import json
from typing import Any, Iterator, Optional

from omnisync.error.exceptions import MalformedDataError
from omnisync.hubspot.models import HubSpotContact
from omnisync.json.pipeline import JsonParsingPipeline

_DEFAULT_PIPELINE = JsonParsingPipeline()


@dataclass(frozen=True)
class HubSpotSearchResult:
    """Parsed container with contacts and cursor token."""

    results: list[HubSpotContact] = field(default_factory=list)
    next_after: Optional[str] = None
    has_more: bool = False


def stream_hubspot_contacts(
    raw_json: str,
    pipeline: Optional[JsonParsingPipeline] = None,
) -> Iterator[HubSpotContact]:
    """Yield parsed HubSpotContact objects lazily from raw JSON response.

    Args:
        raw_json: Raw JSON response payload string.
        pipeline: Optional custom JsonParsingPipeline instance.

    Yields:
        HubSpotContact domain model instances.
    """
    p = pipeline or _DEFAULT_PIPELINE
    return p.stream_array(raw_json, "results", _parse_single_contact)


def parse_hubspot_contact_response(
    raw_json: str,
    pipeline: Optional[JsonParsingPipeline] = None,
) -> HubSpotSearchResult:
    """Parse raw JSON response from HubSpot CRM endpoint defensively.

    Args:
        raw_json: Raw JSON payload string.
        pipeline: Optional custom JsonParsingPipeline instance.

    Returns:
        HubSpotSearchResult containing contacts and next page cursor.

    Raises:
        MalformedDataError: If JSON syntax is invalid or root is not an object.
    """
    p = pipeline or _DEFAULT_PIPELINE
    data = p.parse_dict(raw_json)

    contacts = p.parse_array(raw_json, "results", _parse_single_contact)

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
