package com.parksense.auth;

import java.util.Optional;

/**
 * Per-request identity holder. The auth filter publishes the authenticated
 * user here; deep domain code (e.g. the gate control proxy) reads the role
 * without any HTTP type leaking into the domain.
 */
public final class RequestRoles {

    private static final ThreadLocal<User> CURRENT = new ThreadLocal<>();

    private RequestRoles() {
    }

    public static void set(User user) {
        CURRENT.set(user);
    }

    public static Optional<User> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static boolean isAdmin() {
        return current().map(u -> u.role() == Role.ADMIN).orElse(false);
    }

    public static String currentActor() {
        return current().map(User::username).orElse("system");
    }

    public static void clear() {
        CURRENT.remove();
    }
}
