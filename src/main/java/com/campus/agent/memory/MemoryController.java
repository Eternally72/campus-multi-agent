package com.campus.agent.memory;

import com.campus.agent.common.ApiResponse;
import com.campus.agent.common.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/memory")
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryService memoryService;

    @GetMapping
    ApiResponse<List<MemoryResponse>> list(Authentication authentication) {
        return ApiResponse.ok(memoryService.list(CurrentUser.id(authentication)));
    }

    @GetMapping("/candidates")
    ApiResponse<List<MemoryCandidateResponse>> candidates(Authentication authentication) {
        return ApiResponse.ok(memoryService.listCandidates(CurrentUser.id(authentication)));
    }

    @PostMapping("/candidates/{id}/confirm")
    ApiResponse<Void> confirmCandidate(Authentication authentication, @PathVariable Long id) {
        memoryService.confirmCandidate(CurrentUser.id(authentication), id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/candidates/{id}/reject")
    ApiResponse<Void> rejectCandidate(Authentication authentication, @PathVariable Long id) {
        memoryService.rejectCandidate(CurrentUser.id(authentication), id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/preferences/{id}")
    ApiResponse<Void> forgetPreference(Authentication authentication, @PathVariable Long id) {
        memoryService.forgetPreference(CurrentUser.id(authentication), id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/facts/{id}")
    ApiResponse<Void> forgetFact(Authentication authentication, @PathVariable Long id) {
        memoryService.forgetFact(CurrentUser.id(authentication), id);
        return ApiResponse.ok(null);
    }
}
