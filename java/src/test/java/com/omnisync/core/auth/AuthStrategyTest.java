package com.omnisync.core.auth;

import com.omnisync.core.error.AuthenticationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthStrategyTest {

    @Test
    @DisplayName("BasicAuthStrategy produces correct basic auth headers")
    void basicAuthHeadersSuccess() {
        BasicAuthStrategy strategy = new BasicAuthStrategy("admin@example.com", "secretToken");
        strategy.authenticate();

        Map<String, String> headers = strategy.getAuthHeaders();
        assertThat(headers).containsKey("Authorization");
        assertThat(headers.get("Authorization")).startsWith("Basic ");
        assertThat(strategy.isExpired()).isFalse();
        assertThat(strategy.getUsername()).isEqualTo("admin@example.com");

        strategy.refresh();
        assertThat(strategy.getAuthHeaders()).isEqualTo(headers);
    }

    @Test
    @DisplayName("BasicAuthStrategy throws AuthenticationException on empty credentials")
    void basicAuthEmptyCredentials() {
        BasicAuthStrategy emptyUser = new BasicAuthStrategy("", "secret");
        assertThatThrownBy(emptyUser::authenticate)
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Username and password must not be empty");

        BasicAuthStrategy nullPass = new BasicAuthStrategy("user", null);
        assertThatThrownBy(nullPass::getAuthHeaders)
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    @DisplayName("OAuth2Strategy manages valid token lifecycle")
    void oauth2StrategyLifecycle() {
        OAuth2Strategy strategy = new OAuth2Strategy("client-123", "secret-456", "https://auth.example.com/token");
        Instant future = Instant.now().plusSeconds(3600);
        strategy.setTokens("initial-access-token", "initial-refresh-token", future);

        assertThat(strategy.isExpired()).isFalse();
        assertThat(strategy.getAuthHeaders()).containsEntry("Authorization", "Bearer initial-access-token");
        assertThat(strategy.getClientId()).isEqualTo("client-123");
        assertThat(strategy.getClientSecret()).isEqualTo("secret-456");
        assertThat(strategy.getTokenEndpoint()).isEqualTo("https://auth.example.com/token");
        assertThat(strategy.getAccessToken()).contains("initial-access-token");
        assertThat(strategy.getRefreshToken()).contains("initial-refresh-token");
        assertThat(strategy.getExpiresAt()).contains(future);
    }

    @Test
    @DisplayName("OAuth2Strategy refreshes token when expired or missing")
    void oauth2StrategyRefreshWhenExpired() {
        OAuth2Strategy strategy = new OAuth2Strategy("client-123", "secret-456", "https://auth.example.com/token");
        Instant past = Instant.now().minusSeconds(10);
        strategy.setTokens("expired-token", "refresh-token-xyz", past);

        assertThat(strategy.isExpired()).isTrue();
        strategy.authenticate();

        assertThat(strategy.isExpired()).isFalse();
        assertThat(strategy.getAccessToken()).contains("refreshed_refresh-token-xyz");
        assertThat(strategy.getAuthHeaders()).containsEntry("Authorization", "Bearer refreshed_refresh-token-xyz");
    }

    @Test
    @DisplayName("OAuth2Strategy fails without tokens or client credentials")
    void oauth2StrategyFailures() {
        OAuth2Strategy missingClient = new OAuth2Strategy("", "secret", "https://auth.example.com");
        assertThatThrownBy(missingClient::authenticate)
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("valid clientId");

        OAuth2Strategy noTokens = new OAuth2Strategy("id", "secret", "https://auth.example.com");
        assertThatThrownBy(noTokens::authenticate)
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("No OAuth2 access token");

        assertThatThrownBy(noTokens::refresh)
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("refresh token is absent");
    }
}
