package com.campus.agent.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCourseRequest(
    @NotBlank @Size(max = 120) String name,
    @Size(max = 32) String term,
    @Size(max = 120) String teacher
) {
}
