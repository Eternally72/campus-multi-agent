package com.campus.agent.auth;

public record AuthResponse(String token, Long userId, String username, String displayName, String role) {
}
