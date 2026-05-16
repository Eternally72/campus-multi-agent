package com.campus.agent.memory;

import java.time.Instant;

public record MemoryCandidateResponse(
    Long id,
    MemoryType type,
    String key,
    String value,
    String category,
    Double confidence,
    String reason,
    String source,
    Instant expiresAt,
    Instant createdAt
) {

    static MemoryCandidateResponse from(MemoryCandidate candidate) {
        return new MemoryCandidateResponse(
            candidate.getId(),
            candidate.getMemoryType(),
            candidate.getMemoryKey(),
            candidate.getMemoryValue(),
            candidate.getCategory(),
            candidate.getConfidence(),
            candidate.getReason(),
            candidate.getSource(),
            candidate.getExpiresAt(),
            candidate.getCreatedAt()
        );
    }
}
