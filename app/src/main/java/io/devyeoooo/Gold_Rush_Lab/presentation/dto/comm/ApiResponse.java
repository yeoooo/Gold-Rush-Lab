package io.devyeoooo.Gold_Rush_Lab.presentation.dto.comm;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorResponse error;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static ApiResponse<Void> success() {
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return ApiResponse.<Void>builder()
                .success(false)
                .error(new ErrorResponse(code, message))
                .build();
    }

    public static <T> ApiResponse<T> fail(String code, String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .data(data)
                .error(new ErrorResponse(code, message))
                .build();
    }
}