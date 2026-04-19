package com.example.platform.kernel.exception;

import com.example.platform.kernel.ui.RestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler that translate exceptions into standard RestResponse.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<RestResponse<Void>> handleBusinessException(BusinessException e) {
        return ResponseEntity.badRequest().body(RestResponse.error(e.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<RestResponse<Void>> handleNotFoundException(ResourceNotFoundException e) {
        return ResponseEntity.status(404).body(RestResponse.error(e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RestResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(403).body(RestResponse.error("Bạn không có quyền thực hiện hành động này."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestResponse<Void>> handleGenericException(Exception e) {
        log.error("[GlobalError] Unhandled exception occurred: ", e);
        return ResponseEntity.status(500).body(RestResponse.error("Đã có lỗi hệ thống xảy ra. Vui lòng thử lại sau."));
    }
}
