package com.omnisync.hubspot;

import com.omnisync.core.auth.OAuth2Strategy;
import com.omnisync.core.error.AuthenticationException;
import com.omnisync.core.error.RateLimitExceededException;
import com.omnisync.core.http.HttpClient;
import com.omnisync.core.http.HttpRequest;
import com.omnisync.core.http.HttpResponse;
import com.omnisync.core.pagination.Page;
import com.omnisync.hubspot.model.HubSpotContact;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HubSpotConnectorTest {

    @Mock
    private HttpClient mockHttpClient;

    private OAuth2Strategy createAuthStrategy() {
        OAuth2Strategy auth = new OAuth2Strategy("client-id", "client-secret", "https://api.hubapi.com/oauth/v1/token");
        auth.setTokens("mock-access-token", "mock-refresh-token", Instant.now().plusSeconds(3600));
        return auth;
    }

    @Test
    @DisplayName("HubSpotConnector fetchPage sends proper headers, limit, and properties without cursor initially")
    void fetchInitialPageBuildsCorrectRequest() {
        String pageJson = """
                {
                  "results": [
                    { "id": "1", "properties": { "email": "a@example.com", "firstname": "A" } }
                  ],
                  "paging": {
                    "next": { "after": "cursor-2" }
                  }
                }
                """;
        when(mockHttpClient.execute(any())).thenReturn(new HttpResponse(200, Map.of(), pageJson));

        OAuth2Strategy auth = createAuthStrategy();
        HubSpotConnector connector = new HubSpotConnector("https://api.hubapi.com", auth, mockHttpClient, "contacts", 10, "email,firstname");

        Page<HubSpotContact> page = connector.fetchPage(null);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).execute(captor.capture());
        HttpRequest executed = captor.getValue();

        assertThat(executed.getUrl()).isEqualTo("https://api.hubapi.com/crm/v3/objects/contacts");
        assertThat(executed.getQueryParams()).containsEntry("limit", "10");
        assertThat(executed.getQueryParams()).containsEntry("properties", "email,firstname");
        assertThat(executed.getQueryParams()).doesNotContainKey("after");
        assertThat(executed.getHeaders()).containsEntry("Authorization", "Bearer mock-access-token");

        assertThat(page.hasNext()).isTrue();
        assertThat(page.getNextCursor()).contains("cursor-2");
        assertThat(page.records()).hasSize(1);
        assertThat(page.records().get(0).email()).isEqualTo("a@example.com");
    }

    @Test
    @DisplayName("HubSpotConnector paginate streams across multiple cursor-based pages end-to-end")
    void paginateStreamsAcrossCursorPages() {
        String page1 = """
                {
                  "results": [
                    { "id": "101", "properties": { "email": "user1@hub.com" } }
                  ],
                  "paging": {
                    "next": { "after": "cursor-102" }
                  }
                }
                """;
        String page2 = """
                {
                  "results": [
                    { "id": "102", "properties": { "email": "user2@hub.com" } }
                  ]
                }
                """;

        when(mockHttpClient.execute(any()))
                .thenReturn(new HttpResponse(200, Map.of(), page1))
                .thenReturn(new HttpResponse(200, Map.of(), page2));

        OAuth2Strategy auth = createAuthStrategy();
        HubSpotConnector connector = new HubSpotConnector("https://api.hubapi.com/", auth, mockHttpClient);

        List<String> emails = new ArrayList<>();
        for (HubSpotContact contact : connector.paginate()) {
            emails.add(contact.email());
        }

        assertThat(emails).containsExactly("user1@hub.com", "user2@hub.com");
        assertThat(connector.getBaseUrl()).isEqualTo("https://api.hubapi.com");
        assertThat(connector.getObjectType()).isEqualTo("contacts");
        assertThat(connector.getPageSize()).isEqualTo(50);
    }

    @Test
    @DisplayName("HubSpotConnector propagates HTTP errors from client")
    void propagatesHttpFailures() {
        when(mockHttpClient.execute(any()))
                .thenThrow(new RateLimitExceededException("Rate limit", Duration.ofSeconds(10)))
                .thenThrow(new AuthenticationException("Token invalid"));

        OAuth2Strategy auth = createAuthStrategy();
        HubSpotConnector connector = new HubSpotConnector("https://api.hubapi.com", auth, mockHttpClient);

        assertThatThrownBy(() -> connector.fetchPage(null))
                .isInstanceOf(RateLimitExceededException.class);

        assertThatThrownBy(() -> connector.fetchPage(null))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    @DisplayName("HubSpotConnector validates constructor parameters")
    void validatesConstructorParameters() {
        OAuth2Strategy auth = createAuthStrategy();
        assertThatThrownBy(() -> new HubSpotConnector(null, auth, mockHttpClient))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("baseUrl must not be null");

        assertThatThrownBy(() -> new HubSpotConnector("https://api.hubapi.com", null, mockHttpClient))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new HubSpotConnector("https://api.hubapi.com", auth, null))
                .isInstanceOf(NullPointerException.class);
    }
}
