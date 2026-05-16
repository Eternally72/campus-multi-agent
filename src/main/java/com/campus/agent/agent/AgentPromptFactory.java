package com.campus.agent.agent;

import com.campus.agent.rag.RagSearchResult;
import com.campus.agent.memory.MemoryContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class AgentPromptFactory {

    private final Map<AgentType, String> systemPrompts;

    public AgentPromptFactory() {
        this.systemPrompts = new EnumMap<>(AgentType.class);
        systemPrompts.put(AgentType.ROUTER, load("router-agent.md"));
        systemPrompts.put(AgentType.COURSE_QA, load("course-qa-agent.md"));
        systemPrompts.put(AgentType.STUDY_PLAN, load("study-plan-agent.md"));
        systemPrompts.put(AgentType.MATERIAL_SUMMARY, load("material-summary-agent.md"));
        systemPrompts.put(AgentType.CAMPUS_AFFAIRS, load("campus-affairs-agent.md"));
        systemPrompts.put(AgentType.REFLECTION, load("reflection-agent.md"));
    }

    public String systemPrompt(AgentType type) {
        return systemPrompts.get(type);
    }

    public String userPrompt(String message, List<RagSearchResult> references, MemoryContext memoryContext) {
        StringBuilder builder = new StringBuilder();
        appendMemory(builder, memoryContext);
        builder.append("用户问题：\n").append(message).append("\n\n");
        if (references.isEmpty()) {
            builder.append("知识库检索结果：暂无可用上下文。\n");
        } else {
            builder.append("知识库检索结果：\n");
            for (int i = 0; i < references.size(); i++) {
                builder.append("[").append(i + 1).append("] ")
                    .append("title=").append(references.get(i).title())
                    .append(", materialId=").append(references.get(i).materialId())
                    .append(", chunk=").append(references.get(i).chunkIndex())
                    .append(", score=").append(references.get(i).score())
                    .append("\n")
                    .append(references.get(i).content()).append("\n");
            }
        }
        builder.append("\n请用中文回答，结构清晰，避免编造。");
        return builder.toString();
    }

    private void appendMemory(StringBuilder builder, MemoryContext memoryContext) {
        if (memoryContext == null) {
            return;
        }
        builder.append("可用记忆上下文：\n");
        if (!memoryContext.longTermPreferences().isEmpty()) {
            builder.append("- 长期偏好：").append(String.join("；", memoryContext.longTermPreferences())).append("\n");
        }
        if (!memoryContext.facts().isEmpty()) {
            builder.append("- 用户事实：").append(String.join("；", memoryContext.facts())).append("\n");
        }
        if (memoryContext.mediumSummary() != null && !memoryContext.mediumSummary().isBlank()) {
            builder.append("- 会话摘要：").append(memoryContext.mediumSummary()).append("\n");
        }
        if (!memoryContext.shortTermMessages().isEmpty()) {
            builder.append("- 当前会话最近消息：\n");
            memoryContext.shortTermMessages().forEach(item -> builder.append("  - ").append(item).append("\n"));
        }
        builder.append("\n");
    }

    private String load(String filename) {
        ClassPathResource resource = new ClassPathResource("system-prompt/" + filename);
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load system prompt: " + filename, exception);
        }
    }
}
