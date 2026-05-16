package com.campus.agent.common;

public record ApiResponse<T>(boolean success, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "ok", data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, "created", data);
    }
}
