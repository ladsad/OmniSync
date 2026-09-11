package com.omnisync.jira;

import com.omnisync.core.auth.BasicAuthStrategy;
import com.omnisync.core.error.AuthenticationException;
import com.omnisync.core.error.RateLimitExceededException;
import com.omnisync.core.http.HttpClient;
import com.omnisync.core.http.HttpRequest;
import com.omnisync.core.http.HttpResponse;
import com.omnisync.core.pagination.Page;
import com.omnisync.jira.model.JiraIssue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JiraConnectorTest {

    @Mock
    private HttpClient mockHttpClient;

    private final BasicAuthStrategy authStrategy = new BasicAuthStrategy("user@example.com", "token123");

    @Test
    @DisplayName("JiraConnector fetchPage requests search API with correct query parameters and auth")
    void fetchPageBuildsCorrectRequest() {
        String pageJson = """
                {
                  "startAt": 0,
                  "maxResults": 1,
                  "total": 2,
                  "issues": [
                    {
                      "id": "101",
                      "key": "OPS-1",
                      "fields": { "summary": "Deploy cluster", "status": { "name": "Open" } }
                    }
                  ]
                }
                """;

        when(mockHttpClient.execute(any())).thenReturn(new HttpResponse(200, Map.of(), pageJson));

        JiraConnector connector = new JiraConnector("https://myjira.atlassian.net", authStrategy, mockHttpClient, 1, "project=OPS", "summary,status");

        Page<JiraIssue> page = connector.fetchPage(null);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).execute(captor.capture());
        HttpRequest executedReq = captor.getValue();

        assertThat(executedReq.getUrl()).isEqualTo("https://myjira.atlassian.net/rest/api/3/search");
        assertThat(executedReq.getQueryParams()).containsEntry("startAt", "0");
        assertThat(executedReq.getQueryParams()).containsEntry("maxResults", "1");
        assertThat(executedReq.getQueryParams()).containsEntry("jql", "project=OPS");
        assertThat(executedReq.getHeaders()).containsKey("Authorization");

        assertThat(page.hasNext()).isTrue();
        assertThat(page.getNextCursor()).contains("1");
        assertThat(page.records()).hasSize(1);
        assertThat(page.records().get(0).key()).isEqualTo("OPS-1");
    }

    @Test
    @DisplayName("JiraConnector paginate streams across multiple Jira pages end-to-end")
    void paginateStreamsPagesEndToEnd() {
        String page1Json = """
                {
                  "startAt": 0,
                  "maxResults": 2,
                  "total": 3,
                  "issues": [
                    { "id": "1", "key": "J-1", "fields": { "summary": "Task 1" } },
                    { "id": "2", "key": "J-2", "fields": { "summary": "Task 2" } }
                  ]
                }
                """;

        String page2Json = """
                {
                  "startAt": 2,
                  "maxResults": 2,
                  "total": 3,
                  "issues": [
                    { "id": "3", "key": "J-3", "fields": { "summary": "Task 3" } }
                  ]
                }
                """;

        when(mockHttpClient.execute(any()))
                .thenReturn(new HttpResponse(200, Map.of(), page1Json))
                .thenReturn(new HttpResponse(200, Map.of(), page2Json));

        JiraConnector connector = new JiraConnector("https://myjira.atlassian.net/", authStrategy, mockHttpClient, 2, "", null);

        List<String> issueKeys = new ArrayList<>();
        for (JiraIssue issue : connector.paginate()) {
            issueKeys.add(issue.key());
        }

        assertThat(issueKeys).containsExactly("J-1", "J-2", "J-3");
        assertThat(connector.getBaseUrl()).isEqualTo("https://myjira.atlassian.net");
        assertThat(connector.getPageSize()).isEqualTo(2);
    }

    @Test
    @DisplayName("JiraConnector handles non-integer cursor by falling back to 0")
    void handlesNonIntegerCursor() {
        String pageJson = """
                {
                  "startAt": 0,
                  "maxResults": 50,
                  "total": 0,
                  "issues": []
                }
                """;
        when(mockHttpClient.execute(any())).thenReturn(new HttpResponse(200, Map.of(), pageJson));

        JiraConnector connector = new JiraConnector("https://myjira.atlassian.net", authStrategy, mockHttpClient);
        Page<JiraIssue> page = connector.fetchPage("corrupt-cursor");

        assertThat(page.hasNext()).isFalse();
        assertThat(page.records()).isEmpty();
    }

    @Test
    @DisplayName("JiraConnector propagates HTTP errors from client")
    void propagatesHttpFailures() {
        when(mockHttpClient.execute(any()))
                .thenThrow(new RateLimitExceededException("Rate limit", Duration.ofSeconds(30)))
                .thenThrow(new AuthenticationException("Invalid Jira token"));

        JiraConnector connector = new JiraConnector("https://myjira.atlassian.net", authStrategy, mockHttpClient);

        assertThatThrownBy(() -> connector.fetchPage(null))
                .isInstanceOf(RateLimitExceededException.class);

        assertThatThrownBy(() -> connector.fetchPage(null))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    @DisplayName("JiraConnector validates required parameters")
    void validatesConstructorParameters() {
        assertThatThrownBy(() -> new JiraConnector(null, authStrategy, mockHttpClient))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("baseUrl must not be null");

        assertThatThrownBy(() -> new JiraConnector("https://myjira.net", null, mockHttpClient))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new JiraConnector("https://myjira.net", authStrategy, null))
                .isInstanceOf(NullPointerException.class);
    }
}
