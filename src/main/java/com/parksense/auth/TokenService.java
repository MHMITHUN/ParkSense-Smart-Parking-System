package com.parksense.auth;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bearer-token sessions for the SPA. A login exchanges credentials for an
 * opaque random token; every API call presents it back. Tokens live only
 * in memory, so a restart logs everyone out — the right trade-off for a
 * single-node control room.
 */
public final class TokenService {

    private final Map<String, User> activeTokens = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String issue(User user) {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        activeTokens.put(token, user);
        return token;
    }

    public Optional<User> resolve(String token) {
        return token == null ? Optional.empty() : Optional.ofNullable(activeTokens.get(token));
    }

    public void invalidate(String token) {
        if (token != null) {
            activeTokens.remove(token);
        }
    }

    public int activeCount() {
        return activeTokens.size();
    }
}
