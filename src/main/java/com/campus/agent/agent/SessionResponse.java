package com.campus.agent.agent;

import java.time.Instant;

public record SessionResponse(Long id, String title, Instant createdAt, Instant updatedAt) {

    static SessionResponse from(ChatSession session) {
        return new SessionResponse(session.getId(), session.getTitle(), session.getCreatedAt(), session.getUpdatedAt());
    }
}
