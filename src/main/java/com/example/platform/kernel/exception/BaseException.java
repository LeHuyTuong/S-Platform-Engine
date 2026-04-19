package com.example.platform.kernel.exception;

/**
 * Root exception for the platform in the kernel module.
 */
public class BaseException extends RuntimeException {
    public BaseException(String message) {
        super(message);
    }

    public BaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
