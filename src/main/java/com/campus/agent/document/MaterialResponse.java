package com.campus.agent.document;

import java.time.Instant;

public record MaterialResponse(
    Long id,
    Long courseId,
    String title,
    MaterialStatus status,
    String errorMessage,
    Instant createdAt,
    Instant indexedAt
) {

    static MaterialResponse from(CourseMaterial material) {
        Long courseId = material.getCourse() == null ? null : material.getCourse().getId();
        return new MaterialResponse(
            material.getId(),
            courseId,
            material.getTitle(),
            material.getStatus(),
            material.getErrorMessage(),
            material.getCreatedAt(),
            material.getIndexedAt()
        );
    }
}
