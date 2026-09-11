package com.omnisync.core.auth;

import com.omnisync.core.error.OmniSyncException;

import java.util.Map;

/**
 * Strategy contract for authenticating requests and supplying headers to SaaS APIs.
 */
public interface AuthStrategy {

    /**
     * Authenticates with the upstream provider or validates active credentials.
     *
     * @throws OmniSyncException if authentication credentials are invalid or unavailable
     */
    void authenticate() throws OmniSyncException;

    /**
     * Returns HTTP authorization headers required by the upstream API.
     *
     * @return key-value header mappings to include in HTTP requests
     */
    Map<String, String> getAuthHeaders();

    /**
     * Checks if current authorization credentials or tokens have expired.
     *
     * @return true if credentials need refresh, false otherwise
     */
    boolean isExpired();

    /**
     * Refreshes expired credentials or OAuth2 tokens.
     *
     * @throws OmniSyncException if credential renewal fails
     */
    void refresh() throws OmniSyncException;
}
