package com.omnisync.core.auth;

import com.omnisync.core.error.AuthenticationException;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * OAuth2 token-based authentication strategy managing Bearer tokens and renewal lifecycle.
 */
public class OAuth2Strategy implements AuthStrategy {

    private final String clientId;
    private final String clientSecret;
    private final String tokenEndpoint;

    private String accessToken;
    private String refreshToken;
    private Instant expiresAt;

    /**
     * Constructs an OAuth2Strategy with client credentials and token endpoint details.
     *
     * @param clientId client identifier
     * @param clientSecret client secret
     * @param tokenEndpoint token grant URL
     */
    public OAuth2Strategy(String clientId, String clientSecret, String tokenEndpoint) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenEndpoint = tokenEndpoint;
    }

    /**
     * Sets active OAuth2 token state.
     *
     * @param accessToken active bearer access token
     * @param refreshToken token used to obtain new access tokens
     * @param expiresAt timestamp when access token expires
     */
    public void setTokens(String accessToken, String refreshToken, Instant expiresAt) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
    }

    @Override
    public void authenticate() throws AuthenticationException {
        if (clientId == null || clientId.isBlank() || tokenEndpoint == null || tokenEndpoint.isBlank()) {
            throw new AuthenticationException("OAuth2 configuration requires valid clientId and tokenEndpoint");
        }
        if (accessToken == null) {
            if (refreshToken != null) {
                refresh();
            } else {
                throw new AuthenticationException("No OAuth2 access token or refresh token available");
            }
        } else if (isExpired()) {
            refresh();
        }
    }

    @Override
    public Map<String, String> getAuthHeaders() {
        if (accessToken == null || isExpired()) {
            authenticate();
        }
        return Collections.singletonMap("Authorization", "Bearer " + accessToken);
    }

    @Override
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    @Override
    public void refresh() throws AuthenticationException {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthenticationException("Cannot refresh OAuth2 token: refresh token is absent");
        }
        // Token exchange stub: in real client, performs POST request to tokenEndpoint
        this.accessToken = "refreshed_" + refreshToken;
        this.expiresAt = Instant.now().plusSeconds(3600);
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public Optional<String> getAccessToken() {
        return Optional.ofNullable(accessToken);
    }

    public Optional<String> getRefreshToken() {
        return Optional.ofNullable(refreshToken);
    }

    public Optional<Instant> getExpiresAt() {
        return Optional.ofNullable(expiresAt);
    }
}
