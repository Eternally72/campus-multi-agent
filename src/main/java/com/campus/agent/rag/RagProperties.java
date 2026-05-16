package com.campus.agent.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "campus.rag")
public record RagProperties(
    int chunkSize,
    int chunkOverlap,
    int topK,
    int candidateK,
    double similarityThreshold,
    boolean rerankEnabled,
    double vectorWeight,
    double lexicalWeight
) {

    public RagProperties {
        if (chunkSize < 200) {
            throw new IllegalArgumentException("campus.rag.chunk-size must be at least 200");
        }
        if (chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("campus.rag.chunk-overlap must be >= 0 and less than chunk-size");
        }
        if (topK < 1) {
            throw new IllegalArgumentException("campus.rag.top-k must be at least 1");
        }
        if (candidateK < topK) {
            candidateK = topK;
        }
        if (similarityThreshold < 0 || similarityThreshold > 1) {
            throw new IllegalArgumentException("campus.rag.similarity-threshold must be between 0 and 1");
        }
        if (vectorWeight < 0 || lexicalWeight < 0 || vectorWeight + lexicalWeight <= 0) {
            throw new IllegalArgumentException("campus.rag rerank weights must be non-negative and not both zero");
        }
    }
}
