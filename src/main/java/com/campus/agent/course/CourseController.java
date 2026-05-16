package com.campus.agent.course;

import com.campus.agent.common.ApiResponse;
import com.campus.agent.common.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    ApiResponse<List<CourseResponse>> list(Authentication authentication) {
        return ApiResponse.ok(courseService.list(CurrentUser.id(authentication)));
    }

    @PostMapping
    ApiResponse<CourseResponse> create(Authentication authentication, @Valid @RequestBody CreateCourseRequest request) {
        return ApiResponse.created(courseService.create(CurrentUser.id(authentication), request));
    }
}
