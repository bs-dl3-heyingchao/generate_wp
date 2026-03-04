package com.neusoft.bsdl.wptool.api.dto;

import java.time.Instant;

public record ApiResponse<T>(int code, String message, T data, String timestamp) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "OK", data, Instant.now().toString());
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, Instant.now().toString());
    }
}