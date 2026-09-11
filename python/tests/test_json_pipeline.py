"""Unit tests for centralized JsonParsingPipeline."""

import pytest

from omnisync.error.exceptions import MalformedDataError
from omnisync.hubspot.parser import stream_hubspot_contacts
from omnisync.jira.parser import stream_jira_issues
from omnisync.json.pipeline import JsonParsingPipeline


def test_parse_dict_valid() -> None:
    pipeline = JsonParsingPipeline()
    res = pipeline.parse_dict('{"key": "value", "num": 10}')
    assert res == {"key": "value", "num": 10}


def test_parse_dict_empty_or_whitespace() -> None:
    pipeline = JsonParsingPipeline()
    with pytest.raises(MalformedDataError, match="Empty or null"):
        pipeline.parse_dict("")

    with pytest.raises(MalformedDataError, match="Empty or null"):
        pipeline.parse_dict("   ")


def test_parse_dict_invalid_syntax() -> None:
    pipeline = JsonParsingPipeline()
    with pytest.raises(MalformedDataError, match="Failed to parse JSON"):
        pipeline.parse_dict("{invalid json")


def test_parse_dict_non_dict_root() -> None:
    pipeline = JsonParsingPipeline()
    with pytest.raises(MalformedDataError, match="must be a JSON object"):
        pipeline.parse_dict("[1, 2, 3]")


def test_stream_and_parse_array() -> None:
    pipeline = JsonParsingPipeline()
    raw = """
    {
      "items": [
        {"id": "1", "name": "first"},
        {"id": "skip"},
        "not-a-dict",
        {"id": "2", "name": "second"}
      ]
    }
    """

    def item_parser(item: dict) -> str | None:
        if not isinstance(item, dict):
            return None
        if item.get("id") == "skip":
            return None
        return item.get("name")

    streamed = list(pipeline.stream_array(raw, "items", item_parser))
    parsed = pipeline.parse_array(raw, "items", item_parser)

    assert streamed == ["first", "second"]
    assert parsed == ["first", "second"]


def test_stream_array_missing_or_non_list_field() -> None:
    pipeline = JsonParsingPipeline()
    assert list(pipeline.stream_array("{}", "missing", lambda x: x)) == []
    assert list(pipeline.stream_array('{"key": "not-list"}', "key", lambda x: x)) == []


def test_stream_jira_and_hubspot_generators() -> None:
    jira_json = """
    {
      "issues": [
        {
          "id": "100",
          "key": "DEV-1",
          "fields": {"summary": "Task 1"}
        }
      ]
    }
    """
    jira_items = list(stream_jira_issues(jira_json))
    assert len(jira_items) == 1
    assert jira_items[0].key == "DEV-1"
    assert jira_items[0].summary == "Task 1"

    hubspot_json = """
    {
      "results": [
        {
          "id": "200",
          "properties": {"email": "test@domain.com"}
        }
      ]
    }
    """
    hubspot_items = list(stream_hubspot_contacts(hubspot_json))
    assert len(hubspot_items) == 1
    assert hubspot_items[0].id == "200"
    assert hubspot_items[0].email == "test@domain.com"
