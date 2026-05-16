package com.campus.agent.todo;

import com.campus.agent.common.ApiResponse;
import com.campus.agent.common.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @GetMapping
    ApiResponse<List<TodoResponse>> list(Authentication authentication) {
        return ApiResponse.ok(todoService.list(CurrentUser.id(authentication)));
    }

    @PostMapping
    ApiResponse<TodoResponse> create(Authentication authentication, @Valid @RequestBody CreateTodoRequest request) {
        return ApiResponse.created(todoService.create(CurrentUser.id(authentication), request));
    }

    @PatchMapping("/{id}/status")
    ApiResponse<TodoResponse> status(Authentication authentication, @PathVariable Long id, @RequestParam TodoStatus status) {
        return ApiResponse.ok(todoService.setStatus(CurrentUser.id(authentication), id, status));
    }
}
