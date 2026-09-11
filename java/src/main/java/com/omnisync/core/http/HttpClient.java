package com.omnisync.core.http;

import com.omnisync.core.error.AuthenticationException;
import com.omnisync.core.error.NetworkException;
import com.omnisync.core.error.OmniSyncException;
import com.omnisync.core.error.RateLimitExceededException;

/**
 * Transport contract for making outbound HTTP calls to SaaS endpoints.
 */
public interface HttpClient {

    /**
     * Executes an HTTP request and returns the normalized response.
     *
     * @param request outbound request description
     * @return response returned by the server
     * @throws AuthenticationException if server returns 401 or 403
     * @throws RateLimitExceededException if server returns 429
     * @throws NetworkException on transport/connection error, timeout, or 5xx response
     * @throws OmniSyncException for other protocol-level failures
     */
    HttpResponse execute(HttpRequest request) throws OmniSyncException;
}
