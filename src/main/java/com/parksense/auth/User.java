package com.parksense.auth;

import java.util.Objects;

/**
 * A staff account. Passwords are stored only as PBKDF2 hashes — the plain
 * text never lives anywhere in the system.
 */
public final class User {

    private final String username;
    private final String fullName;
    private final String passwordHash;
    private final Role role;

    public User(String username, String fullName, String passwordHash, Role role) {
        this.username = Objects.requireNonNull(username).trim().toLowerCase();
        this.fullName = Objects.requireNonNull(fullName);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.role = Objects.requireNonNull(role);
    }

    public String username() {
        return username;
    }

    public String fullName() {
        return fullName;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Role role() {
        return role;
    }
}
