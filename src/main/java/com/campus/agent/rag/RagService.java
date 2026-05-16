package com.campus.agent.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(RagProperties.class)
public class RagService {

    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final RagProperties properties;

    public boolean available() {
        return vectorStoreProvider.getIfAvailable() != null;
    }

    public void indexMaterial(Long userId, Long materialId, Long courseId, String title, String content) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            throw new IllegalStateException("向量库未启用，请确认 pgvector 和 EmbeddingModel 配置");
        }

        deleteMaterialVectors(userId, materialId);

        List<Document> documents = new ArrayList<>();
        List<String> chunks = split(content);
        for (int index = 0; index < chunks.size(); index++) {
            String chunkId = chunkId(materialId, index);
            documents.add(new Document(chunkId, chunks.get(index), Map.of(
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
            .topK(resolveCandidateK(topK))
            .similarityThreshold(properties.similarityThreshold())
            .filterExpression(userFilter(userId))
            .build();
        return vectorStore.similaritySearch(request).stream()
            .map(document -> toResult(document, query))
            .sorted(resultComparator())
            .limit(resolveTopK(topK))
            .toList();
    }

    public void deleteMaterialVectors(Long userId, Long materialId) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            return;
        }
        try {
            vectorStore.delete(materialFilter(userId, materialId));
        } catch (RuntimeException exception) {
            throw new IllegalStateException("资料向量删除失败，请稍后重试", exception);
        }
    }

    private List<String> split(String content) {
        String normalized = content.replace("\r\n", "\n").trim();
        if (normalized.isBlank()) {
            return List.of();
        }
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + properties.chunkSize(), normalized.length());
            chunks.add(normalized.substring(start, end));
            if (end == normalized.length()) {
                break;
            }
            start = Math.max(0, end - properties.chunkOverlap());
        }
        return chunks;
    }

    private RagSearchResult toResult(Document document, String query) {
        Map<String, Object> metadata = document.getMetadata();
        Double vectorScore = document.getScore();
        double rerankScore = properties.rerankEnabled()
            ? rerankScore(query, document.getText(), metadata, vectorScore)
            : vectorScore == null ? 0 : vectorScore;
        return new RagSearchResult(
            document.getText(),
            metadata,
            vectorScore,
            rerankScore,
            stringValue(metadata.get("title")),
            stringValue(metadata.get("materialId")),
            stringValue(metadata.get("chunk"))
        );
    }

    private Comparator<RagSearchResult> resultComparator() {
        return properties.rerankEnabled()
            ? Comparator.comparing(RagSearchResult::rerankScore, Comparator.nullsLast(Comparator.reverseOrder()))
            : Comparator.comparing(RagSearchResult::score, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private double rerankScore(String query, String content, Map<String, Object> metadata, Double vectorScore) {
        double normalizedVectorScore = vectorScore == null ? 0 : vectorScore;
        double lexicalScore = lexicalOverlap(query, stringValue(metadata.get("title")) + "\n" + content);
        return properties.vectorWeight() * normalizedVectorScore + properties.lexicalWeight() * lexicalScore;
    }

    private double lexicalOverlap(String query, String content) {
        Set<String> queryTerms = terms(query);
        if (queryTerms.isEmpty()) {
            return 0;
        }
        Set<String> contentTerms = terms(content);
        int hit = 0;
        for (String term : queryTerms) {
            if (contentTerms.contains(term)) {
                hit++;
            }
        }
        return (double) hit / queryTerms.size();
    }

    private Set<String> terms(String value) {
        String normalized = value == null ? "" : value.toLowerCase().replaceAll("[\\p{Punct}\\s]+", "");
        Set<String> terms = new LinkedHashSet<>();
        for (int i = 0; i < normalized.length(); i++) {
            terms.add(String.valueOf(normalized.charAt(i)));
            if (i + 2 <= normalized.length()) {
                terms.add(normalized.substring(i, i + 2));
            }
        }
        return terms;
    }

    private int resolveTopK(int topK) {
        int requested = topK > 0 ? topK : properties.topK();
        return Math.max(1, requested);
    }

    private int resolveCandidateK(int topK) {
        return Math.max(resolveTopK(topK), properties.candidateK());
    }

    private Filter.Expression userFilter(Long userId) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        return builder.eq("userId", String.valueOf(userId)).build();
    }

    private Filter.Expression materialFilter(Long userId, Long materialId) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        return builder.and(
            builder.eq("userId", String.valueOf(userId)),
            builder.eq("materialId", String.valueOf(materialId))
        ).build();
    }

    private String chunkId(Long materialId, int chunkIndex) {
        return "material-" + materialId + "-chunk-" + chunkIndex;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
