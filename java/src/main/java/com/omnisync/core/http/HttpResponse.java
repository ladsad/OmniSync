package com.omnisync.core.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable value representation of an inbound HTTP response.
 */
public final class HttpResponse {

    private final int statusCode;
    private final Map<String, String> headers;
    private final String body;

    public HttpResponse(int statusCode, Map<String, String> headers, String body) {
        this.statusCode = statusCode;
        Map<String, String> normalized = new HashMap<>();
        if (headers != null) {
            headers.forEach((k, v) -> {
                if (k != null) {
                    normalized.put(k.toLowerCase(), v);
                }
            });
        }
        this.headers = Collections.unmodifiableMap(normalized);
        this.body = body != null ? body : "";
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Optional<String> getHeader(String name) {
        return name != null ? Optional.ofNullable(headers.get(name.toLowerCase())) : Optional.empty();
    }

    public String getBody() {
        return body;
    }
}
