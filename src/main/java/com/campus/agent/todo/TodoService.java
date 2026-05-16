package com.campus.agent.todo;

import com.campus.agent.user.AppUser;
import com.campus.agent.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoItemRepository todos;
    private final AppUserRepository users;

    @Transactional
    public TodoResponse create(Long userId, CreateTodoRequest request) {
        AppUser owner = users.getReferenceById(userId);
        TodoItem item = new TodoItem();
        item.setOwner(owner);
        item.setTitle(request.title());
        item.setDescription(request.description());
        item.setDueDate(request.dueDate());
        return TodoResponse.from(todos.save(item));
    }

    @Transactional
    public TodoResponse setStatus(Long userId, Long todoId, TodoStatus status) {
        TodoItem item = ownedTodo(userId, todoId);
        item.setStatus(status);
        return TodoResponse.from(item);
    }

    @Transactional(readOnly = true)
    public List<TodoResponse> list(Long userId) {
        return todos.findByOwnerIdOrderByStatusAscDueDateAscCreatedAtDesc(userId).stream()
            .map(TodoResponse::from)
            .toList();
    }

    private TodoItem ownedTodo(Long userId, Long todoId) {
        TodoItem item = todos.findById(todoId).orElseThrow(() -> new IllegalArgumentException("待办不存在"));
        if (!item.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("无权访问该待办");
        }
        return item;
    }
}
