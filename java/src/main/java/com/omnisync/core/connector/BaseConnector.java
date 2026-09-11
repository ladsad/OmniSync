package com.omnisync.core.connector;

import com.omnisync.core.auth.AuthStrategy;
import com.omnisync.core.error.OmniSyncException;
import com.omnisync.core.pagination.Page;
import com.omnisync.core.pagination.PaginationIterable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Base abstract connector implementing shared auth delegation, record extraction, and pagination.
 *
 * @param <T> domain record type produced by the connector
 */
public abstract class BaseConnector<T> implements Connector<T> {

    protected final AuthStrategy authStrategy;

    /**
     * Initializes BaseConnector with an injected authentication strategy.
     *
     * @param authStrategy authentication strategy implementation
     */
    protected BaseConnector(AuthStrategy authStrategy) {
        this.authStrategy = Objects.requireNonNull(authStrategy, "authStrategy must not be null");
    }

    @Override
    public void authenticate() throws OmniSyncException {
        authStrategy.authenticate();
    }

    @Override
    public List<T> extractRecords(Page<T> page) {
        return page != null ? page.records() : Collections.emptyList();
    }

    @Override
    public void handleError(OmniSyncException error) {
        // Default behavior propagates failure; subclasses can attach retry or telemetry logic
        throw error;
    }

    @Override
    public Iterable<T> paginate() {
        return new PaginationIterable<>(this::fetchPage);
    }

    /**
     * Returns the configured authentication strategy.
     *
     * @return auth strategy
     */
    public AuthStrategy getAuthStrategy() {
        return authStrategy;
    }
}
