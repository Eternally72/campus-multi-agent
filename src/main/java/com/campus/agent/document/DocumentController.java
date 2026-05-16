package com.campus.agent.document;

import com.campus.agent.common.ApiResponse;
import com.campus.agent.common.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    ApiResponse<List<MaterialResponse>> list(Authentication authentication) {
        return ApiResponse.ok(documentService.list(CurrentUser.id(authentication)));
    }

    @PostMapping
    ApiResponse<MaterialResponse> create(Authentication authentication, @Valid @RequestBody CreateMaterialRequest request) {
        return ApiResponse.created(documentService.create(CurrentUser.id(authentication), request));
    }

    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(Authentication authentication, @PathVariable Long id) {
        documentService.delete(CurrentUser.id(authentication), id);
        return ApiResponse.ok(null);
    }
}
