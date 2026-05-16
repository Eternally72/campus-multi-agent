package com.campus.agent.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMaterialRequest(
    Long courseId,
    @NotBlank @Size(max = 180) String title,
    @NotBlank String content
) {
}
