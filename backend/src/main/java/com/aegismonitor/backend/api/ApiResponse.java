package com.aegismonitor.backend.api;

public record ApiResponse<T>(
    boolean success,
    String code,
    String message,
    T data
) {
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, "OK", message, data);
    }
}
