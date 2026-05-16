package com.campus.agent.memory;

import java.time.Instant;

public record MemoryResponse(
    Long id,
    String type,
    String key,
    String value,
    MemoryStatus status,
    Double confidence,
    Instant expiresAt,
    Instant updatedAt
) {

    static MemoryResponse from(UserMemoryPreference preference) {
        return new MemoryResponse(
            preference.getId(),
            "preference",
            preference.getMemoryKey(),
            preference.getMemoryValue(),
            preference.getStatus(),
            preference.getConfidence(),
            null,
            preference.getUpdatedAt()
        );
    }

    static MemoryResponse from(UserMemoryFact fact) {
        return new MemoryResponse(
            fact.getId(),
            "fact",
            fact.getFactKey(),
            fact.getFactValue(),
            fact.getStatus(),
            fact.getConfidence(),
            fact.getExpiresAt(),
            fact.getUpdatedAt()
        );
    }
}
