package com.campus.agent.document;

public record DocumentIndexEvent(Long userId, Long materialId, Long courseId, String title) {
}
