package com.omnisync.core.pagination;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Immutable representation of a single page of records extracted from a SaaS API.
 *
 * @param <T> type of domain record contained in the page
 * @param records records contained within this page
 * @param nextCursor pointer/token for the subsequent page, or null if final page
 * @param hasNext indicator whether additional pages exist
 */
public record Page<T>(List<T> records, String nextCursor, boolean hasNext) {

    public Page {
        records = (records != null) ? List.copyOf(records) : Collections.emptyList();
    }

    /**
     * Constructs a final page with no subsequent pages.
     *
     * @param <T> type of record
     * @param records items present on this terminal page
     * @return a page marked with hasNext=false
     */
    public static <T> Page<T> lastPage(List<T> records) {
        return new Page<>(records, null, false);
    }

    /**
     * Constructs an empty terminal page.
     *
     * @param <T> type of record
     * @return empty page
     */
    public static <T> Page<T> empty() {
        return new Page<>(Collections.emptyList(), null, false);
    }

    /**
     * Returns an Optional containing the cursor for the next page, if present.
     *
     * @return optional cursor string
     */
    public Optional<String> getNextCursor() {
        return Optional.ofNullable(nextCursor);
    }
}
