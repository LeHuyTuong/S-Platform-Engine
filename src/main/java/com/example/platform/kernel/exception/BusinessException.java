package com.example.platform.kernel.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown for business logic violations (e.g., quota exceeded).
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BusinessException extends BaseException {
    public BusinessException(String message) {
        super(message);
    }
}
