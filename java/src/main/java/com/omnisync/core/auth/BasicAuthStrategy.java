package com.omnisync.core.auth;

import com.omnisync.core.error.AuthenticationException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Basic Authentication strategy supplying RFC 7617 base64 authorization headers.
 */
public class BasicAuthStrategy implements AuthStrategy {

    private final String username;
    private final String password;
    private String cachedHeader;

    /**
     * Constructs a BasicAuthStrategy with user credentials.
     *
     * @param username the username or account ID
     * @param password the password or API token
     */
    public BasicAuthStrategy(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void authenticate() throws AuthenticationException {
        if (username == null || username.isBlank() || password == null) {
            throw new AuthenticationException("Username and password must not be empty for Basic Auth");
        }
        String token = username + ":" + password;
        this.cachedHeader = "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Map<String, String> getAuthHeaders() {
        if (cachedHeader == null) {
            authenticate();
        }
        return Collections.singletonMap("Authorization", cachedHeader);
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    @Override
    public void refresh() {
        // Basic auth credentials do not have automated expiry renewal
        authenticate();
    }

    /**
     * Returns the configured username.
     *
     * @return username
     */
    public String getUsername() {
        return username;
    }
}
