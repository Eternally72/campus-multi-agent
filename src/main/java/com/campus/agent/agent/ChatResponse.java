package com.campus.agent.agent;

import com.campus.agent.rag.RagSearchResult;

import java.util.List;

public record ChatResponse(Long sessionId, AgentType agentType, String answer, List<RagSearchResult> references) {
}
