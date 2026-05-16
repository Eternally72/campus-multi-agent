package com.campus.agent.memory;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "campus.memory")
public record MemoryProperties(
    int shortTermTtlMinutes,
    int shortTermMaxMessages,
    int summaryMaxCharacters,
    int factDefaultTtlDays
) {

    public MemoryProperties {
        if (shortTermTtlMinutes < 5) {
            throw new IllegalArgumentException("campus.memory.short-term-ttl-minutes must be at least 5");
        }
        if (shortTermMaxMessages < 2) {
            throw new IllegalArgumentException("campus.memory.short-term-max-messages must be at least 2");
        }
        if (summaryMaxCharacters < 500) {
            throw new IllegalArgumentException("campus.memory.summary-max-characters must be at least 500");
        }
        if (factDefaultTtlDays < 1) {
            throw new IllegalArgumentException("campus.memory.fact-default-ttl-days must be at least 1");
        }
    }
}
