package com.omnisync.jira;

import com.omnisync.core.auth.AuthStrategy;
import com.omnisync.core.connector.BaseConnector;
import com.omnisync.core.error.OmniSyncException;
import com.omnisync.core.http.DefaultHttpClient;
import com.omnisync.core.http.HttpClient;
import com.omnisync.core.http.HttpMethod;
import com.omnisync.core.http.HttpRequest;
import com.omnisync.core.http.HttpResponse;
import com.omnisync.core.pagination.Page;
import com.omnisync.jira.model.JiraIssue;
import com.omnisync.jira.parser.JiraIssueParser;
import com.omnisync.jira.parser.JiraSearchResult;

import java.util.Objects;

/**
 * Concrete SaaS connector for extracting issues from Jira REST API.
 */
public class JiraConnector extends BaseConnector<JiraIssue> {

    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final String DEFAULT_JQL = "ORDER BY created DESC";
    public static final String DEFAULT_FIELDS = "summary,status,issuetype,priority,created,updated,assignee";

    private final String baseUrl;
    private final HttpClient httpClient;
    private final JiraIssueParser parser;
    private final int pageSize;
    private final String jql;
    private final String fields;

    /**
     * Constructs a JiraConnector with default HTTP client and settings.
     *
     * @param baseUrl base URL of the Jira instance (e.g. https://example.atlassian.net)
     * @param authStrategy authentication strategy (BasicAuth or OAuth2)
     */
    public JiraConnector(String baseUrl, AuthStrategy authStrategy) {
        this(baseUrl, authStrategy, new DefaultHttpClient(), DEFAULT_PAGE_SIZE, DEFAULT_JQL, DEFAULT_FIELDS);
    }

    /**
     * Constructs a JiraConnector with custom HTTP client.
     *
     * @param baseUrl base URL of the Jira instance
     * @param authStrategy authentication strategy
     * @param httpClient HTTP client transport
     */
    public JiraConnector(String baseUrl, AuthStrategy authStrategy, HttpClient httpClient) {
        this(baseUrl, authStrategy, httpClient, DEFAULT_PAGE_SIZE, DEFAULT_JQL, DEFAULT_FIELDS);
    }

    /**
     * Constructs a fully-configured JiraConnector.
     *
     * @param baseUrl base URL of the Jira instance
     * @param authStrategy authentication strategy
     * @param httpClient HTTP client transport
     * @param pageSize number of issues per page (maxResults)
     * @param jql Jira Query Language filter
     * @param fields comma-separated list of issue fields to retrieve
     */
    public JiraConnector(
            String baseUrl,
            AuthStrategy authStrategy,
            HttpClient httpClient,
            int pageSize,
            String jql,
            String fields
    ) {
        super(authStrategy);
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl must not be null").replaceAll("/+$", "");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.parser = new JiraIssueParser();
        this.pageSize = pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE;
        this.jql = jql != null ? jql : DEFAULT_JQL;
        this.fields = fields != null ? fields : DEFAULT_FIELDS;
    }

    @Override
    public Page<JiraIssue> fetchPage(String cursor) throws OmniSyncException {
        int startAt = 0;
        if (cursor != null && !cursor.isBlank()) {
            try {
                startAt = Integer.parseInt(cursor.trim());
            } catch (NumberFormatException ignored) {
                startAt = 0;
            }
        }

        HttpRequest.Builder reqBuilder = HttpRequest.builder()
                .url(baseUrl + "/rest/api/3/search")
                .method(HttpMethod.GET)
                .headers(authStrategy.getAuthHeaders())
                .queryParam("startAt", String.valueOf(startAt))
                .queryParam("maxResults", String.valueOf(pageSize))
                .queryParam("fields", fields);

        if (!jql.isBlank()) {
            reqBuilder.queryParam("jql", jql);
        }

        HttpResponse response = httpClient.execute(reqBuilder.build());
        JiraSearchResult result = parser.parseSearchResponse(response.getBody());

        int nextStartAt = result.startAt() + result.issues().size();
        boolean hasNext = nextStartAt < result.total() && !result.issues().isEmpty();
        String nextCursor = hasNext ? String.valueOf(nextStartAt) : null;

        return new Page<>(result.issues(), nextCursor, hasNext);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getPageSize() {
        return pageSize;
    }

    public String getJql() {
        return jql;
    }
}
