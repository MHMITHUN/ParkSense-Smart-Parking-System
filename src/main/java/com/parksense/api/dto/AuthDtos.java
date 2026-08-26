package com.parksense.api.dto;

/**
 * Auth request/response shapes. Java records serialise straight to JSON,
 * so the DTO layer stays declarative.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(String username, String password) {
    }

    public record AuthResponse(String token, String username, String fullName, String role) {
    }

    public record MeResponse(String username, String fullName, String role) {
    }
}
