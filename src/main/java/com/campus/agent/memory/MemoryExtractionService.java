package com.campus.agent.memory;

import com.campus.agent.agent.AiGateway;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemoryExtractionService {

    private final AiGateway aiGateway;
    private final ObjectMapper objectMapper;

    public List<MemoryExtractionCandidate> extract(String userMessage, MemoryContext memoryContext) {
        List<MemoryExtractionCandidate> llmCandidates = extractWithLlm(userMessage, memoryContext);
        return llmCandidates.isEmpty() ? List.of() : llmCandidates;
    }

    private List<MemoryExtractionCandidate> extractWithLlm(String userMessage, MemoryContext memoryContext) {
        String response = aiGateway.complete(loadPrompt(), buildUserPrompt(userMessage, memoryContext));
        try {
            ExtractedMemoryResponse parsed = objectMapper.readValue(extractJson(response), ExtractedMemoryResponse.class);
            if (parsed.candidates() == null) {
                return List.of();
            }
            List<MemoryExtractionCandidate> result = new ArrayList<>();
            for (ExtractedMemoryCandidate candidate : parsed.candidates()) {
                if (candidate.type() == null || isBlank(candidate.key()) || isBlank(candidate.value())) {
                    continue;
                }
                result.add(new MemoryExtractionCandidate(
                    candidate.type(),
                    candidate.key(),
                    candidate.value(),
                    isBlank(candidate.category()) ? defaultCategory(candidate.type()) : candidate.category(),
                    Math.max(0, Math.min(1, candidate.confidence())),
                    candidate.reason(),
                    candidate.ttlDays(),
                    "llm"
                ));
            }
            return result;
        } catch (RuntimeException | IOException exception) {
            return List.of();
        }
    }

    private String buildUserPrompt(String userMessage, MemoryContext memoryContext) {
        return """
            已有长期偏好：%s
            已有事实记忆：%s
            用户最新消息：
            %s
            """.formatted(
            memoryContext.longTermPreferences(),
            memoryContext.facts(),
            userMessage
        );
    }

    private String loadPrompt() {
        try {
            return new ClassPathResource("system-prompt/memory-extractor.md").getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load memory extractor prompt", exception);
        }
    }

    private String extractJson(String value) {
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return value.substring(start, end + 1);
        }
        return "{\"candidates\":[]}";
    }

    private String defaultCategory(MemoryType type) {
        return type == MemoryType.PREFERENCE ? "preference" : "fact";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ExtractedMemoryResponse(List<ExtractedMemoryCandidate> candidates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ExtractedMemoryCandidate(
        MemoryType type,
        String key,
        String value,
        String category,
        double confidence,
        String reason,
        Integer ttlDays
    ) {
    }
}
