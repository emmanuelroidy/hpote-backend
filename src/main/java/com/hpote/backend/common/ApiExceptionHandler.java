package com.hpote.backend.common;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    

    @ExceptionHandler(ApiExceptionDto.class)
    public ResponseEntity<ApiResponseDto<Void>> handleApiException(ApiExceptionDto ex) {

        return ResponseEntity
                .badRequest()
                .body(ApiResponseDto.error(ex.getMessage()));
    }

}
