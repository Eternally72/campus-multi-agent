package com.campus.agent.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RagService {

    private static final int CHUNK_SIZE = 900;
    private static final int CHUNK_OVERLAP = 120;

    private final ObjectProvider<VectorStore> vectorStoreProvider;

    public boolean available() {
        return vectorStoreProvider.getIfAvailable() != null;
    }

    public void indexMaterial(Long userId, Long materialId, Long courseId, String title, String content) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            throw new IllegalStateException("向量库未启用，请确认 pgvector 和 EmbeddingModel 配置");
        }

        List<Document> documents = new ArrayList<>();
        List<String> chunks = split(content);
        for (int index = 0; index < chunks.size(); index++) {
            documents.add(new Document(chunks.get(index), Map.of(
                "userId", String.valueOf(userId),
                "materialId", String.valueOf(materialId),
                "courseId", courseId == null ? "" : String.valueOf(courseId),
                "title", title,
                "chunk", String.valueOf(index)
            )));
        }
        vectorStore.add(documents);
    }

    public List<RagSearchResult> search(Long userId, String query, int topK) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            return List.of();
        }

        SearchRequest request = SearchRequest.builder()
            .query(query)
            .topK(Math.max(1, Math.min(topK, 8)))
            .filterExpression("userId == '" + userId + "'")
            .build();
        return vectorStore.similaritySearch(request).stream()
            .map(document -> new RagSearchResult(document.getText(), document.getMetadata()))
            .toList();
    }

    private List<String> split(String content) {
        String normalized = content.replace("\r\n", "\n").trim();
        if (normalized.isBlank()) {
            return List.of();
        }
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + CHUNK_SIZE, normalized.length());
            chunks.add(normalized.substring(start, end));
            if (end == normalized.length()) {
                break;
            }
            start = Math.max(0, end - CHUNK_OVERLAP);
        }
        return chunks;
    }
}
