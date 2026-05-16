package com.campus.agent.agent;

import java.time.Instant;

public record MessageResponse(Long id, String role, AgentType agentType, String content, Instant createdAt) {

    static MessageResponse from(ChatMessage message) {
        return new MessageResponse(message.getId(), message.getRole(), message.getAgentType(), message.getContent(), message.getCreatedAt());
    }
}
