"""Unit tests for the HubSpot CRM response parser."""

import pytest

from omnisync.error import MalformedDataError
from omnisync.hubspot.parser import parse_hubspot_contact_response


def test_parse_valid_hubspot_response() -> None:
    payload = """
    {
      "results": [
        {
          "id": "1001",
          "properties": {
            "email": "alex@example.com",
            "firstname": "Alex",
            "lastname": "Rider",
            "company": "MI6",
            "phone": "555-1234"
          },
          "createdAt": "2026-09-01T00:00:00Z",
          "updatedAt": "2026-09-02T00:00:00Z",
          "archived": false
        }
      ],
      "paging": {
        "next": {
          "after": "cursor-999"
        }
      }
    }
    """
    res = parse_hubspot_contact_response(payload)
    assert res.has_more is True
    assert res.next_after == "cursor-999"
    assert len(res.results) == 1

    contact = res.results[0]
    assert contact.id == "1001"
    assert contact.email == "alex@example.com"
    assert contact.first_name == "Alex"
    assert contact.last_name == "Rider"
    assert contact.company == "MI6"
    assert contact.phone == "555-1234"
    assert contact.created_at == "2026-09-01T00:00:00Z"
    assert contact.updated_at == "2026-09-02T00:00:00Z"
    assert contact.archived is False


def test_parse_hubspot_defensive_skipping() -> None:
    payload = """
    {
      "results": [
        {
          "id": "1002",
          "properties": null
        },
        {
          "missing_id": true
        },
        null
      ]
    }
    """
    res = parse_hubspot_contact_response(payload)
    assert res.has_more is False
    assert res.next_after is None
    assert len(res.results) == 1
    assert res.results[0].id == "1002"
    assert res.results[0].email == ""


def test_parse_hubspot_failures() -> None:
    with pytest.raises(MalformedDataError, match="Empty or null"):
        parse_hubspot_contact_response("")

    with pytest.raises(MalformedDataError, match="Failed to parse"):
        parse_hubspot_contact_response("{invalid json")

    with pytest.raises(MalformedDataError, match="root must be a JSON object"):
        parse_hubspot_contact_response("[]")
