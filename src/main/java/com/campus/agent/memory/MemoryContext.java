package com.campus.agent.memory;

import java.util.List;

public record MemoryContext(
    List<String> shortTermMessages,
    String mediumSummary,
    List<String> longTermPreferences,
    List<String> facts
) {
}
