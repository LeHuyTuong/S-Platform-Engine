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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public ApiAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        RestResponse<Void> body = new RestResponse<>();
        body.setSuccess(false);
        body.setMessage("Authentication required.");
        body.setPath(request.getRequestURI());
        body.setError(new ApiError("UNAUTHORIZED", "Authentication required."));
        Object requestId = request.getAttribute(RestResponseBodyAdvice.REQUEST_ID_ATTRIBUTE);
        if (requestId != null) {
            body.setRequestId(String.valueOf(requestId));
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
