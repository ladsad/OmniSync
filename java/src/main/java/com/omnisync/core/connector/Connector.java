package com.omnisync.core.connector;

import com.omnisync.core.error.OmniSyncException;
import com.omnisync.core.pagination.Page;

import java.util.List;

/**
 * Root contract defining standardized SaaS integration operations.
 *
 * @param <T> domain record type produced by the connector
 */
public interface Connector<T> {

    /**
     * Performs authentication handshakes or verifies active credentials.
     *
     * @throws OmniSyncException if authentication fails
     */
    void authenticate() throws OmniSyncException;

    /**
     * Fetches a single page of domain items given a pagination cursor.
     *
     * @param cursor token/offset for target page, or null for initial page
     * @return page object containing records and next cursor
     * @throws OmniSyncException if HTTP or payload processing fails
     */
    Page<T> fetchPage(String cursor) throws OmniSyncException;

    /**
     * Extracts and normalizes domain records from a retrieved page payload.
     *
     * @param page source page container
     * @return list of extracted typed records
     */
    List<T> extractRecords(Page<T> page);

    /**
     * Standard error handler for handling or classifying extraction exceptions.
     *
     * @param error the caught exception
     */
    void handleError(OmniSyncException error);

    /**
     * Returns an Iterable stream traversing all records across all available pages.
     *
     * @return record iterable
     */
    Iterable<T> paginate();
}
