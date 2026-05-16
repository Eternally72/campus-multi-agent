package com.campus.agent.rag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DashScopeRerankClient implements RerankClient {

    private final RagProperties properties;

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    @Override
    public List<RagSearchResult> rerank(String query, List<RagSearchResult> candidates, int topK) {
        if (apiKey == null || apiKey.isBlank() || candidates.isEmpty()) {
            throw new IllegalStateException("DashScope reranker is not configured");
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(properties.rerankerTimeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.rerankerTimeoutSeconds()));

        RerankRequest request = new RerankRequest(
            properties.rerankerModel(),
            candidates.stream().map(RagSearchResult::content).toList(),
            query,
            Math.min(topK, candidates.size()),
            properties.rerankerInstruction()
        );

        RerankResponse response = RestClient.builder()
            .requestFactory(requestFactory)
            .build()
            .post()
            .uri(properties.rerankerEndpoint())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + apiKey)
            .body(request)
            .retrieve()
            .body(RerankResponse.class);

        if (response == null || response.results() == null || response.results().isEmpty()) {
            throw new IllegalStateException("DashScope reranker returned no results");
        }

        return response.results().stream()
            .filter(result -> result.index() >= 0 && result.index() < candidates.size())
            .sorted(Comparator.comparing(RerankResult::relevanceScore, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(topK)
            .map(result -> withRerankScore(candidates.get(result.index()), result.relevanceScore()))
            .toList();
    }

    private RagSearchResult withRerankScore(RagSearchResult result, Double score) {
        return new RagSearchResult(
            result.content(),
            result.metadata(),
            result.score(),
            score,
            "dashscope:" + properties.rerankerModel(),
            result.title(),
            result.materialId(),
            result.chunkIndex()
        );
    }

    private record RerankRequest(
        String model,
        List<String> documents,
        String query,
        @JsonProperty("top_n") int topN,
        String instruct
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RerankResponse(List<RerankResult> results, Map<String, Object> usage) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RerankResult(int index, @JsonProperty("relevance_score") Double relevanceScore) {
    }
}
