package com.campus.agent.agent;

import com.campus.agent.common.ApiResponse;
import com.campus.agent.common.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentOrchestrator orchestrator;

    @PostMapping("/chat")
    ApiResponse<ChatResponse> chat(Authentication authentication, @Valid @RequestBody ChatRequest request) {
        return ApiResponse.ok(orchestrator.chat(CurrentUser.id(authentication), request));
    }

    @GetMapping("/sessions")
    ApiResponse<List<SessionResponse>> sessions(Authentication authentication) {
        return ApiResponse.ok(orchestrator.listSessions(CurrentUser.id(authentication)));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    ApiResponse<List<MessageResponse>> messages(Authentication authentication, @PathVariable Long sessionId) {
        return ApiResponse.ok(orchestrator.listMessages(CurrentUser.id(authentication), sessionId));
    }
}
