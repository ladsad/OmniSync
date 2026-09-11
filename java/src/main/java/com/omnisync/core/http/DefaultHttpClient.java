package com.omnisync.core.http;

import com.omnisync.core.error.AuthenticationException;
import com.omnisync.core.error.NetworkException;
import com.omnisync.core.error.OmniSyncException;
import com.omnisync.core.error.RateLimitExceededException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Standard implementation of HttpClient backed by Java 11+ java.net.http.HttpClient.
 */
public class DefaultHttpClient implements HttpClient {

    private final java.net.http.HttpClient underlyingClient;

    /**
     * Constructs a DefaultHttpClient using the default system java.net.http.HttpClient.
     */
    public DefaultHttpClient() {
        this(java.net.http.HttpClient.newBuilder()
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build());
    }

    /**
     * Constructs a DefaultHttpClient wrapping an explicit java.net.http.HttpClient instance.
     *
     * @param underlyingClient configured java.net.http.HttpClient
     */
    public DefaultHttpClient(java.net.http.HttpClient underlyingClient) {
        this.underlyingClient = Objects.requireNonNull(underlyingClient, "underlyingClient must not be null");
    }

    @Override
    public HttpResponse execute(HttpRequest request) throws OmniSyncException {
        Objects.requireNonNull(request, "request must not be null");

        URI uri = buildUri(request.getUrl(), request.getQueryParams());
        java.net.http.HttpRequest.Builder builder = java.net.http.HttpRequest.newBuilder()
                .uri(uri)
                .timeout(request.getTimeout());

        request.getHeaders().forEach(builder::header);

        java.net.http.HttpRequest.BodyPublisher publisher = request.getBody()
                .map(java.net.http.HttpRequest.BodyPublishers::ofString)
                .orElseGet(java.net.http.HttpRequest.BodyPublishers::noBody);

        switch (request.getMethod()) {
            case GET -> builder.GET();
            case POST -> builder.POST(publisher);
            case PUT -> builder.PUT(publisher);
            case DELETE -> builder.DELETE();
            case PATCH -> builder.method("PATCH", publisher);
        }

        try {
            java.net.http.HttpResponse<String> response = underlyingClient.send(
                    builder.build(),
                    java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            return handleResponse(response);
        } catch (HttpTimeoutException e) {
            throw new NetworkException("Request timed out: " + e.getMessage(), null, e);
        } catch (IOException e) {
            throw new NetworkException("Transport network failure: " + e.getMessage(), null, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NetworkException("HTTP request execution interrupted", null, e);
        }
    }

    protected HttpResponse handleResponse(java.net.http.HttpResponse<String> response) {
        int status = response.statusCode();
        String body = response.body() != null ? response.body() : "";

        Map<String, String> headers = new HashMap<>();
        response.headers().map().forEach((k, v) -> {
            if (k != null && !v.isEmpty()) {
                headers.put(k.toLowerCase(), v.get(0));
            }
        });

        if (status == 401 || status == 403) {
            throw new AuthenticationException("Authentication failed (HTTP " + status + "): " + body);
        }

        if (status == 429) {
            Duration retryAfter = parseRetryAfter(headers.get("retry-after"));
            throw new RateLimitExceededException("Rate limit exceeded (HTTP 429): " + body, retryAfter);
        }

        if (status >= 500) {
            throw new NetworkException("Upstream server error (HTTP " + status + "): " + body, status);
        }

        if (status >= 400) {
            throw new NetworkException("HTTP client error (HTTP " + status + "): " + body, status);
        }

        return new HttpResponse(status, headers, body);
    }

    protected Duration parseRetryAfter(String retryAfterHeader) {
        if (retryAfterHeader == null || retryAfterHeader.isBlank()) {
            return null;
        }
        try {
            long seconds = Long.parseLong(retryAfterHeader.trim());
            return Duration.ofSeconds(seconds);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private URI buildUri(String baseUrl, Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return URI.create(baseUrl);
        }
        StringJoiner joiner = new StringJoiner("&");
        queryParams.forEach((k, v) -> joiner.add(
                URLEncoder.encode(k, StandardCharsets.UTF_8) + "=" +
                        URLEncoder.encode(v != null ? v : "", StandardCharsets.UTF_8)
        ));
        String separator = baseUrl.contains("?") ? "&" : "?";
        return URI.create(baseUrl + separator + joiner);
    }
}
