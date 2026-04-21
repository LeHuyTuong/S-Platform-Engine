package com.example.platform.kernel.exception;

import com.example.platform.kernel.ui.ApiError;
import com.example.platform.kernel.ui.ApiFieldError;
import com.example.platform.kernel.ui.RestResponse;
import com.example.platform.kernel.ui.RestResponseBodyAdvice;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Global exception handler that translate exceptions into standard RestResponse.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<RestResponse<Void>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "BUSINESS_ERROR", e.getMessage(), request, List.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<RestResponse<Void>> handleNotFoundException(ResourceNotFoundException e, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage(), request, List.of());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<RestResponse<Void>> handleUnauthorizedException(UnauthorizedException e, HttpServletRequest request) {
        return buildError(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", e.getMessage(), request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RestResponse<Void>> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        return buildError(HttpStatus.FORBIDDEN, "FORBIDDEN",
                "Ban khong co quyen thuc hien hanh dong nay.", request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestResponse<Void>> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        List<ApiFieldError> fieldErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .toList();
        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Du lieu dau vao khong hop le.", request, fieldErrors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RestResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        ApiFieldError fieldError = new ApiFieldError(e.getName(), "Gia tri khong dung dinh dang.", e.getValue());
        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Tham so request khong hop le.", request, List.of(fieldError));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<RestResponse<Void>> handleBadCredentials(BadCredentialsException e, HttpServletRequest request) {
        return buildError(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                "Email hoac mat khau khong dung.", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestResponse<Void>> handleGenericException(Exception e, HttpServletRequest request) {
        log.error("[GlobalError] Unhandled exception occurred: ", e);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Da co loi he thong xay ra. Vui long thu lai sau.", request, List.of());
    }

    private ApiFieldError toFieldError(FieldError fieldError) {
        return new ApiFieldError(fieldError.getField(), fieldError.getDefaultMessage(), fieldError.getRejectedValue());
    }

    private ResponseEntity<RestResponse<Void>> buildError(HttpStatus status,
                                                          String code,
                                                          String message,
                                                          HttpServletRequest request,
                                                          List<ApiFieldError> fieldErrors) {
        RestResponse<Void> response = new RestResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setPath(request.getRequestURI());
        response.setTimestamp(LocalDateTime.now());
        Object requestId = request.getAttribute(RestResponseBodyAdvice.REQUEST_ID_ATTRIBUTE);
        if (requestId != null) {
            response.setRequestId(String.valueOf(requestId));
        }
        ApiError error = new ApiError(code, message);
        error.setFieldErrors(fieldErrors);
        response.setError(error);
        return ResponseEntity.status(status).body(response);
    }
}
