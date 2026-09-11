package com.omnisync.core.http;

import com.omnisync.core.error.AuthenticationException;
import com.omnisync.core.error.NetworkException;
import com.omnisync.core.error.RateLimitExceededException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpClientTest {

    private static HttpServer server;
    private static String baseUrl;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        server.createContext("/ok", exchange -> {
            byte[] response = "{\"result\":\"success\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        server.createContext("/unauthorized", exchange -> {
            byte[] response = "Unauthorized access".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(401, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        server.createContext("/rate-limited", exchange -> {
            byte[] response = "Too many requests".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Retry-After", "120");
            exchange.sendResponseHeaders(429, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        server.createContext("/server-error", exchange -> {
            byte[] response = "Internal error".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        server.createContext("/echo-method", exchange -> {
            exchange.getResponseHeaders().set("X-Echo-Method", exchange.getRequestMethod());
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });

        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("HttpRequest model and builder store fields and defaults")
    void httpRequestBuilder() {
        HttpRequest req = HttpRequest.builder()
                .url("https://api.example.com/items")
                .method(HttpMethod.POST)
                .header("Content-Type", "application/json")
                .headers(Map.of("X-Trace", "123"))
                .queryParam("page", "1")
                .queryParams(Map.of("limit", "50"))
                .body("{\"key\":\"value\"}")
                .timeout(Duration.ofSeconds(10))
                .build();

        assertThat(req.getUrl()).isEqualTo("https://api.example.com/items");
        assertThat(req.getMethod()).isEqualTo(HttpMethod.POST);
        assertThat(req.getHeaders()).containsEntry("Content-Type", "application/json").containsEntry("X-Trace", "123");
        assertThat(req.getQueryParams()).containsEntry("page", "1").containsEntry("limit", "50");
        assertThat(req.getBody()).contains("{\"key\":\"value\"}");
        assertThat(req.getTimeout()).isEqualTo(Duration.ofSeconds(10));

        HttpRequest getReq = HttpRequest.get("https://api.example.com/items");
        assertThat(getReq.getMethod()).isEqualTo(HttpMethod.GET);
        assertThat(getReq.getTimeout()).isEqualTo(Duration.ofSeconds(30));

        HttpRequest postReq = HttpRequest.post("https://api.example.com/items", "payload");
        assertThat(postReq.getMethod()).isEqualTo(HttpMethod.POST);
        assertThat(postReq.getBody()).contains("payload");
    }

    @Test
    @DisplayName("HttpResponse handles status and case-insensitive headers")
    void httpResponseModel() {
        HttpResponse res = new HttpResponse(200, Map.of("Content-Type", "application/json"), "{\"status\":\"ok\"}");
        assertThat(res.getStatusCode()).isEqualTo(200);
        assertThat(res.isSuccessful()).isTrue();
        assertThat(res.getBody()).isEqualTo("{\"status\":\"ok\"}");
        assertThat(res.getHeader("content-type")).contains("application/json");
        assertThat(res.getHeader("CONTENT-TYPE")).contains("application/json");
        assertThat(res.getHeader("absent")).isEmpty();
        assertThat(res.getHeader(null)).isEmpty();

        HttpResponse nullHeaders = new HttpResponse(500, null, null);
        assertThat(nullHeaders.isSuccessful()).isFalse();
        assertThat(nullHeaders.getBody()).isEmpty();
    }

    @Test
    @DisplayName("DefaultHttpClient executes successful request against server")
    void defaultHttpClientSuccess() {
        DefaultHttpClient client = new DefaultHttpClient();
        HttpRequest request = HttpRequest.builder()
                .url(baseUrl + "/ok")
                .queryParam("cursor", "next123")
                .header("Accept", "application/json")
                .method(HttpMethod.GET)
                .build();

        HttpResponse response = client.execute(request);
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo("{\"result\":\"success\"}");
        assertThat(response.getHeader("content-type")).contains("application/json");
    }

    @Test
    @DisplayName("DefaultHttpClient translates 401 and 403 to AuthenticationException")
    void defaultHttpClientAuthFailure() {
        DefaultHttpClient client = new DefaultHttpClient();
        HttpRequest request = HttpRequest.get(baseUrl + "/unauthorized");

        assertThatThrownBy(() -> client.execute(request))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("HTTP 401");
    }

    @Test
    @DisplayName("DefaultHttpClient translates 429 to RateLimitExceededException with Retry-After")
    void defaultHttpClientRateLimit() {
        DefaultHttpClient client = new DefaultHttpClient();
        HttpRequest request = HttpRequest.get(baseUrl + "/rate-limited");

        assertThatThrownBy(() -> client.execute(request))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(ex -> {
                    RateLimitExceededException rle = (RateLimitExceededException) ex;
                    assertThat(rle.getRetryAfter()).contains(Duration.ofSeconds(120));
                });
    }

    @Test
    @DisplayName("DefaultHttpClient translates 5xx and 4xx to NetworkException")
    void defaultHttpClientServerError() {
        DefaultHttpClient client = new DefaultHttpClient();
        assertThatThrownBy(() -> client.execute(HttpRequest.get(baseUrl + "/server-error")))
                .isInstanceOf(NetworkException.class)
                .satisfies(ex -> assertThat(((NetworkException) ex).getStatusCode()).hasValue(500));

        assertThatThrownBy(() -> client.execute(HttpRequest.get(baseUrl + "/non-existent-endpoint")))
                .isInstanceOf(NetworkException.class)
                .satisfies(ex -> assertThat(((NetworkException) ex).getStatusCode()).hasValue(404));
    }

    @Test
    @DisplayName("DefaultHttpClient translates connection failures and timeouts to NetworkException")
    void defaultHttpClientTransportFailures() {
        DefaultHttpClient client = new DefaultHttpClient();

        // Connect to a port where no server is listening
        HttpRequest unroutable = HttpRequest.get("http://localhost:1");
        assertThatThrownBy(() -> client.execute(unroutable))
                .isInstanceOf(NetworkException.class);
    }

    @Test
    @DisplayName("DefaultHttpClient handles all HTTP methods (POST, PUT, PATCH, DELETE)")
    void defaultHttpClientHttpMethods() {
        DefaultHttpClient client = new DefaultHttpClient();

        HttpRequest post = HttpRequest.post(baseUrl + "/echo-method", "data");
        HttpResponse postRes = client.execute(post);
        assertThat(postRes.getHeader("x-echo-method")).contains("POST");

        HttpRequest put = HttpRequest.builder().url(baseUrl + "/echo-method").method(HttpMethod.PUT).body("data").build();
        HttpResponse putRes = client.execute(put);
        assertThat(putRes.getHeader("x-echo-method")).contains("PUT");

        HttpRequest patch = HttpRequest.builder().url(baseUrl + "/echo-method").method(HttpMethod.PATCH).body("data").build();
        HttpResponse patchRes = client.execute(patch);
        assertThat(patchRes.getHeader("x-echo-method")).contains("PATCH");

        HttpRequest delete = HttpRequest.builder().url(baseUrl + "/echo-method").method(HttpMethod.DELETE).build();
        HttpResponse delRes = client.execute(delete);
        assertThat(delRes.getHeader("x-echo-method")).contains("DELETE");
    }
}
