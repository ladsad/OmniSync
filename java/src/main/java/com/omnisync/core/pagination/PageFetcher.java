package com.omnisync.core.pagination;

import com.omnisync.core.error.OmniSyncException;

/**
 * Functional contract for retrieving a page of items using a cursor/offset token.
 *
 * @param <T> type of domain record returned
 */
@FunctionalInterface
public interface PageFetcher<T> {

    /**
     * Fetches a single page of results for the given cursor.
     *
     * @param cursor cursor or pagination token; null indicates the first page
     * @return page of items
     * @throws OmniSyncException if retrieval fails
     */
    Page<T> fetch(String cursor) throws OmniSyncException;
}
