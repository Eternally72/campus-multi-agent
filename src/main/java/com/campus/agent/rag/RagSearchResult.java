package com.campus.agent.rag;

import java.util.Map;

public record RagSearchResult(
    String content,
    Map<String, Object> metadata,
    Double score,
    Double rerankScore,
    String title,
    String materialId,
    String chunkIndex
) {
}
