package com.omnisync.core.pagination;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaginationIteratorTest {

    @Test
    @DisplayName("PaginationIterator streams multiple pages sequentially")
    void iteratesMultiplePages() {
        PageFetcher<String> fetcher = cursor -> {
            if (cursor == null) {
                return new Page<>(List.of("item-1", "item-2"), "page-2", true);
            } else if ("page-2".equals(cursor)) {
                return new Page<>(List.of("item-3", "item-4"), "page-3", true);
            } else {
                return Page.lastPage(List.of("item-5"));
            }
        };

        PaginationIterator<String> iterator = new PaginationIterator<>(fetcher);
        List<String> items = new ArrayList<>();
        while (iterator.hasNext()) {
            items.add(iterator.next());
        }

        assertThat(items).containsExactly("item-1", "item-2", "item-3", "item-4", "item-5");
        assertThat(iterator.hasNext()).isFalse();
        assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("PaginationIterable supports enhanced for-each loops")
    void iterableForEachLoop() {
        PageFetcher<Integer> fetcher = cursor -> {
            if (cursor == null) {
                return new Page<>(List.of(10, 20), "c2", true);
            }
            return Page.lastPage(List.of(30));
        };

        PaginationIterable<Integer> iterable = new PaginationIterable<>(fetcher);
        List<Integer> result = new ArrayList<>();
        for (Integer num : iterable) {
            result.add(num);
        }

        assertThat(result).containsExactly(10, 20, 30);
    }

    @Test
    @DisplayName("Pagination handles empty pages and null page responses gracefully")
    void handlesEmptyAndNullPages() {
        PageFetcher<String> emptyFetcher = cursor -> Page.empty();
        PaginationIterator<String> emptyIter = new PaginationIterator<>(emptyFetcher);
        assertThat(emptyIter.hasNext()).isFalse();
        assertThatThrownBy(emptyIter::next).isInstanceOf(NoSuchElementException.class);

        PageFetcher<String> nullFetcher = cursor -> null;
        PaginationIterator<String> nullIter = new PaginationIterator<>(nullFetcher);
        assertThat(nullIter.hasNext()).isFalse();

        Page<String> page = Page.lastPage(List.of("single"));
        assertThat(page.getNextCursor()).isEmpty();
        assertThat(page.hasNext()).isFalse();
    }
}
