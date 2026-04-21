package com.example.platform.modules.user.infrastructure;

import com.example.platform.kernel.ui.ApiError;
import com.example.platform.kernel.ui.RestResponse;
import com.example.platform.kernel.ui.RestResponseBodyAdvice;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ApiAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        RestResponse<Void> body = new RestResponse<>();
        body.setSuccess(false);
        body.setMessage("Access denied.");
        body.setPath(request.getRequestURI());
        body.setError(new ApiError("FORBIDDEN", "Access denied."));
        Object requestId = request.getAttribute(RestResponseBodyAdvice.REQUEST_ID_ATTRIBUTE);
        if (requestId != null) {
            body.setRequestId(String.valueOf(requestId));
        }

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
