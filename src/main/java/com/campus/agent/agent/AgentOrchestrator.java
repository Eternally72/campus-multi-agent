package com.campus.agent.agent;

import com.campus.agent.rag.RagSearchResult;
import com.campus.agent.rag.RagService;
import com.campus.agent.memory.MemoryContext;
import com.campus.agent.memory.MemoryService;
import com.campus.agent.user.AppUser;
import com.campus.agent.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final ChatSessionRepository sessions;
    private final ChatMessageRepository messages;
    private final AppUserRepository users;
    private final RouterAgent routerAgent;
    private final AgentPromptFactory promptFactory;
    private final AiGateway aiGateway;
    private final RagService ragService;
    private final MemoryService memoryService;

    @Transactional
    public ChatResponse chat(Long userId, ChatRequest request) {
        ChatSession session = resolveSession(userId, request);
        AgentType agentType = routerAgent.route(request.message());

        ChatMessage userMessage = new ChatMessage();
        userMessage.setSession(session);
        userMessage.setRole("user");
        userMessage.setAgentType(agentType);
        userMessage.setContent(request.message());
        messages.save(userMessage);

        MemoryContext memoryContext = memoryService.buildContext(userId, session.getId());
        List<RagSearchResult> references = ragService.search(userId, request.message(), 5);
        String answer = aiGateway.complete(
            promptFactory.systemPrompt(agentType),
            promptFactory.userPrompt(request.message(), references, memoryContext)
        );

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setSession(session);
        assistantMessage.setRole("assistant");
        assistantMessage.setAgentType(agentType);
        assistantMessage.setContent(answer);
        messages.save(assistantMessage);

        session.setUpdatedAt(Instant.now());
        memoryService.afterTurn(userId, session, userMessage, assistantMessage);
        return new ChatResponse(session.getId(), agentType, answer, references);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> listSessions(Long userId) {
        return sessions.findByOwnerIdOrderByUpdatedAtDesc(userId).stream()
            .map(SessionResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> listMessages(Long userId, Long sessionId) {
        ChatSession session = ownedSession(userId, sessionId);
        return messages.findBySessionIdOrderByCreatedAtAsc(session.getId()).stream()
            .map(MessageResponse::from)
            .toList();
    }

    private ChatSession resolveSession(Long userId, ChatRequest request) {
        if (request.sessionId() != null) {
            return ownedSession(userId, request.sessionId());
        }

        AppUser owner = users.getReferenceById(userId);
        ChatSession session = new ChatSession();
        session.setOwner(owner);
        session.setTitle(title(request.message()));
        return sessions.save(session);
    }

    private ChatSession ownedSession(Long userId, Long sessionId) {
        ChatSession session = sessions.findById(sessionId).orElseThrow(() -> new IllegalArgumentException("会话不存在"));
        if (!session.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("无权访问该会话");
        }
        return session;
    }

    private String title(String message) {
        String text = message.replaceAll("\\s+", " ").trim();
        return text.length() <= 32 ? text : text.substring(0, 32);
    }
}
