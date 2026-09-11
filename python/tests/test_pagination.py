"""Unit tests for the pagination abstraction."""

from typing import Optional
import pytest

from omnisync.pagination import Page, PaginationIterator, paginate


def test_pagination_iterator_multi_page() -> None:
    def fetcher(cursor: Optional[str]) -> Page[str]:
        if cursor is None:
            return Page(records=["item-1", "item-2"], next_cursor="page-2", has_next=True)
        elif cursor == "page-2":
            return Page(records=["item-3", "item-4"], next_cursor="page-3", has_next=True)
        else:
            return Page(records=["item-5"], next_cursor=None, has_next=False)

    iterator = PaginationIterator(fetcher)
    assert iter(iterator) is iterator
    records = list(iterator)
    assert records == ["item-1", "item-2", "item-3", "item-4", "item-5"]
    assert iterator.next_cursor is None

    with pytest.raises(StopIteration):
        next(iterator)


def test_paginate_helper() -> None:
    def fetcher(cursor: Optional[str]) -> Page[int]:
        if cursor is None:
            return Page(records=[1, 2], next_cursor="c2", has_next=True)
        return Page(records=[3], next_cursor=None, has_next=False)

    records = list(paginate(fetcher))
    assert records == [1, 2, 3]


def test_pagination_empty_and_null_responses() -> None:
    empty_iter = PaginationIterator(lambda cursor: Page())
    assert list(empty_iter) == []

    null_iter = PaginationIterator(lambda cursor: None)
    assert list(null_iter) == []
