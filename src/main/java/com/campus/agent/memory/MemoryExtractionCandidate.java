package com.campus.agent.memory;

public record MemoryExtractionCandidate(
    MemoryType type,
    String key,
    String value,
    String category,
    double confidence,
    String reason,
    Integer ttlDays,
    String source
) {
}
