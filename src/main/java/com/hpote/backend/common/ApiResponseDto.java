package com.hpote.backend.common;

public class ApiResponseDto<T> {

    private boolean success;
    private int statusCode;
    private String message;
    private T data;

    public ApiResponseDto(boolean success, int statusCode, String message, T data) {
        this.success = success;
        this.statusCode = statusCode;
        this.message = message;
        this.data = data;
    }

    // ---------- GETTERS / SETTERS ----------
    public boolean isSuccess() {
        return success;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    // ---------- FACTORY METHODS ----------

    // ✅ SUCCESS RESPONSE
    public static <T> ApiResponseDto<T> success(T data) {
        return new ApiResponseDto<>(
                true,
                200,
                "Success",
                data
        );
    }

    // ✅ ERROR RESPONSE (THIS FIXES YOUR BUG)
    public static <T> ApiResponseDto<T> error(String message) {
        return new ApiResponseDto<>(
                false,
                400,
                message,
                null
        );
    }
}
