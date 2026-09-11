package com.omnisync.core.http;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable value representation of an outbound HTTP request.
 */
public final class HttpRequest {

    private final String url;
    private final HttpMethod method;
    private final Map<String, String> headers;
    private final Map<String, String> queryParams;
    private final String body;
    private final Duration timeout;

    private HttpRequest(Builder builder) {
        this.url = Objects.requireNonNull(builder.url, "url must not be null");
        this.method = Objects.requireNonNull(builder.method, "method must not be null");
        this.headers = Collections.unmodifiableMap(new HashMap<>(builder.headers));
        this.queryParams = Collections.unmodifiableMap(new HashMap<>(builder.queryParams));
        this.body = builder.body;
        this.timeout = builder.timeout != null ? builder.timeout : Duration.ofSeconds(30);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static HttpRequest get(String url) {
        return builder().url(url).method(HttpMethod.GET).build();
    }

    public static HttpRequest post(String url, String body) {
        return builder().url(url).method(HttpMethod.POST).body(body).build();
    }

    public String getUrl() {
        return url;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public Optional<String> getBody() {
        return Optional.ofNullable(body);
    }

    public Duration getTimeout() {
        return timeout;
    }

    /**
     * Builder for constructing immutable HttpRequest instances.
     */
    public static final class Builder {
        private String url;
        private HttpMethod method = HttpMethod.GET;
        private final Map<String, String> headers = new HashMap<>();
        private final Map<String, String> queryParams = new HashMap<>();
        private String body;
        private Duration timeout;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder method(HttpMethod method) {
            this.method = method;
            return this;
        }

        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            if (headers != null) {
                this.headers.putAll(headers);
            }
            return this;
        }

        public Builder queryParam(String key, String value) {
            this.queryParams.put(key, value);
            return this;
        }

        public Builder queryParams(Map<String, String> queryParams) {
            if (queryParams != null) {
                this.queryParams.putAll(queryParams);
            }
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public HttpRequest build() {
            return new HttpRequest(this);
        }
    }
}
