package com.hpote.backend.common;

public class ApiExceptionDto extends RuntimeException {
    public ApiExceptionDto(String message) {
        super(message);
    }
}
