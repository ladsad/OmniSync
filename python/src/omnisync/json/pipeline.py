"""Centralized high-throughput JSON parsing pipeline for OmniSync."""

import json
from typing import Any, Callable, Iterator, Optional, TypeVar

from omnisync.error.exceptions import MalformedDataError

T = TypeVar("T")


class JsonParsingPipeline:
    """Centralized JSON deserialization and defensive extraction pipeline."""

    def parse_dict(self, raw_json: str) -> dict[str, Any]:
        """Parse raw JSON string into a dictionary root defensively.

        Args:
            raw_json: Raw JSON payload string.

        Returns:
            Parsed dictionary.

        Raises:
            MalformedDataError: If input is null, empty, syntax is invalid, or root is not a dict.
        """
        if not raw_json or not raw_json.strip():
            raise MalformedDataError("Empty or null JSON payload", raw_payload=raw_json)

        try:
            data = json.loads(raw_json)
        except Exception as exc:
            raise MalformedDataError(
                f"Failed to parse JSON payload: {exc}",
                raw_payload=raw_json,
            ) from exc

        if not isinstance(data, dict):
            raise MalformedDataError(
                "JSON response root must be a JSON object",
                raw_payload=raw_json,
            )

        return data

    def stream_array(
        self,
        raw_json: str,
        array_key: str,
        item_parser: Callable[[Any], Optional[T]],
    ) -> Iterator[T]:
        """Stream parsed items from a nested array field lazily.

        Args:
            raw_json: Raw JSON payload string.
            array_key: Key inside the root object containing the list of items.
            item_parser: Callable converting a raw item into a domain model, or None if skipped.

        Yields:
            Parsed domain model instances.

        Raises:
            MalformedDataError: If root JSON structure is invalid.
        """
        data = self.parse_dict(raw_json)
        items_raw = data.get(array_key, [])
        if isinstance(items_raw, list):
            for raw_item in items_raw:
                parsed = item_parser(raw_item)
                if parsed is not None:
                    yield parsed

    def parse_array(
        self,
        raw_json: str,
        array_key: str,
        item_parser: Callable[[Any], Optional[T]],
    ) -> list[T]:
        """Extract and parse all items from a nested array defensively into a list.

        Args:
            raw_json: Raw JSON payload string.
            array_key: Key inside the root object containing the list of items.
            item_parser: Callable converting a raw item into a domain model, or None if skipped.

        Returns:
            List of parsed domain model instances.
        """
        return list(self.stream_array(raw_json, array_key, item_parser))
