package com.omnisync.hubspot;

import com.omnisync.core.auth.AuthStrategy;
import com.omnisync.core.connector.BaseConnector;
import com.omnisync.core.error.OmniSyncException;
import com.omnisync.core.http.DefaultHttpClient;
import com.omnisync.core.http.HttpClient;
import com.omnisync.core.http.HttpMethod;
import com.omnisync.core.http.HttpRequest;
import com.omnisync.core.http.HttpResponse;
import com.omnisync.core.pagination.Page;
import com.omnisync.hubspot.model.HubSpotContact;
import com.omnisync.hubspot.parser.HubSpotContactParser;
import com.omnisync.hubspot.parser.HubSpotSearchResult;

import java.util.Objects;

/**
 * Concrete SaaS connector for extracting CRM objects (e.g. Contacts) from HubSpot REST API v3.
 */
public class HubSpotConnector extends BaseConnector<HubSpotContact> {

    public static final String DEFAULT_BASE_URL = "https://api.hubapi.com";
    public static final String DEFAULT_OBJECT_TYPE = "contacts";
    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final String DEFAULT_PROPERTIES = "email,firstname,lastname,company,phone";

    private final String baseUrl;
    private final HttpClient httpClient;
    private final HubSpotContactParser parser;
    private final String objectType;
    private final int pageSize;
    private final String properties;

    /**
     * Constructs a HubSpotConnector with default cloud endpoint and default HTTP client.
     *
     * @param authStrategy authentication strategy (OAuth2Strategy or private app token)
     */
    public HubSpotConnector(AuthStrategy authStrategy) {
        this(DEFAULT_BASE_URL, authStrategy, new DefaultHttpClient(), DEFAULT_OBJECT_TYPE, DEFAULT_PAGE_SIZE, DEFAULT_PROPERTIES);
    }

    /**
     * Constructs a HubSpotConnector with custom base URL and HTTP client.
     *
     * @param baseUrl base URL of HubSpot API (e.g. https://api.hubapi.com)
     * @param authStrategy authentication strategy
     * @param httpClient HTTP client transport
     */
    public HubSpotConnector(String baseUrl, AuthStrategy authStrategy, HttpClient httpClient) {
        this(baseUrl, authStrategy, httpClient, DEFAULT_OBJECT_TYPE, DEFAULT_PAGE_SIZE, DEFAULT_PROPERTIES);
    }

    /**
     * Constructs a fully-configured HubSpotConnector.
     *
     * @param baseUrl base URL of HubSpot API
     * @param authStrategy authentication strategy
     * @param httpClient HTTP client transport
     * @param objectType CRM object type (e.g. "contacts", "companies", "deals")
     * @param pageSize page batch limit
     * @param properties comma-separated properties to request
     */
    public HubSpotConnector(
            String baseUrl,
            AuthStrategy authStrategy,
            HttpClient httpClient,
            String objectType,
            int pageSize,
            String properties
    ) {
        super(authStrategy);
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl must not be null").replaceAll("/+$", "");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.parser = new HubSpotContactParser();
        this.objectType = (objectType != null && !objectType.isBlank()) ? objectType : DEFAULT_OBJECT_TYPE;
        this.pageSize = pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE;
        this.properties = (properties != null && !properties.isBlank()) ? properties : DEFAULT_PROPERTIES;
    }

    @Override
    public Page<HubSpotContact> fetchPage(String cursor) throws OmniSyncException {
        HttpRequest.Builder reqBuilder = HttpRequest.builder()
                .url(baseUrl + "/crm/v3/objects/" + objectType)
                .method(HttpMethod.GET)
                .headers(authStrategy.getAuthHeaders())
                .queryParam("limit", String.valueOf(pageSize))
                .queryParam("properties", properties)
                .queryParam("archived", "false");

        if (cursor != null && !cursor.isBlank()) {
            reqBuilder.queryParam("after", cursor.trim());
        }

        HttpResponse response = httpClient.execute(reqBuilder.build());
        HubSpotSearchResult result = parser.parseContactResponse(response.getBody());

        return new Page<>(result.results(), result.nextAfter(), result.hasMore());
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getObjectType() {
        return objectType;
    }

    public int getPageSize() {
        return pageSize;
    }
}
