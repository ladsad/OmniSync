package com.omnisync.core.pagination;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Lazy, streaming Iterator that traverses multi-page SaaS results transparently.
 *
 * @param <T> type of domain record
 */
public class PaginationIterator<T> implements Iterator<T> {

    private final PageFetcher<T> fetcher;
    private String nextCursor;
    private boolean hasMorePages = true;
    private Iterator<T> currentRecordIterator = Collections.emptyIterator();

    /**
     * Constructs a PaginationIterator backed by a PageFetcher.
     *
     * @param fetcher callback invoked to fetch pages
     */
    public PaginationIterator(PageFetcher<T> fetcher) {
        this.fetcher = Objects.requireNonNull(fetcher, "fetcher must not be null");
    }

    @Override
    public boolean hasNext() {
        while (!currentRecordIterator.hasNext() && hasMorePages) {
            Page<T> page = fetcher.fetch(nextCursor);
            if (page == null) {
                hasMorePages = false;
                break;
            }
            this.currentRecordIterator = page.records().iterator();
            this.nextCursor = page.nextCursor();
            this.hasMorePages = page.hasNext();
        }
        return currentRecordIterator.hasNext();
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more records available across paginated stream");
        }
        return currentRecordIterator.next();
    }

    /**
     * Returns the current cursor state.
     *
     * @return current pagination cursor
     */
    public String getNextCursor() {
        return nextCursor;
    }
}
