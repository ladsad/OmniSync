package com.omnisync.core.pagination;

import java.util.Iterator;
import java.util.Objects;

/**
 * Iterable adapter wrapping a PageFetcher for enhanced for-loop and Stream traversal.
 *
 * @param <T> type of domain record
 */
public class PaginationIterable<T> implements Iterable<T> {

    private final PageFetcher<T> fetcher;

    /**
     * Constructs a PaginationIterable with the provided PageFetcher.
     *
     * @param fetcher callback invoked to retrieve pages
     */
    public PaginationIterable(PageFetcher<T> fetcher) {
        this.fetcher = Objects.requireNonNull(fetcher, "fetcher must not be null");
    }

    @Override
    public Iterator<T> iterator() {
        return new PaginationIterator<>(fetcher);
    }
}
