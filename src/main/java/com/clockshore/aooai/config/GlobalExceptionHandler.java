package com.clockshore.aooai.config;

import com.clockshore.aooai.common.ApiResponse;
import com.clockshore.aooai.enumType.ResultCode;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handle(Exception e) {
        return ApiResponse.failure(ResultCode.FAIL, e.getMessage());
    }
}
