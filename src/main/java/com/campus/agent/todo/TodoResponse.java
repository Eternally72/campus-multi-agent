package com.campus.agent.todo;

import java.time.Instant;
import java.time.LocalDate;

public record TodoResponse(
    Long id,
    String title,
    String description,
    LocalDate dueDate,
    TodoStatus status,
    Instant createdAt
) {

    static TodoResponse from(TodoItem item) {
        return new TodoResponse(item.getId(), item.getTitle(), item.getDescription(), item.getDueDate(), item.getStatus(), item.getCreatedAt());
    }
}
