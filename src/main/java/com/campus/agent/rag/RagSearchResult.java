package com.campus.agent.rag;

import java.util.Map;

public record RagSearchResult(String content, Map<String, Object> metadata) {
}
