package com.example.platform.kernel.ui;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.time.LocalDateTime;

@ControllerAdvice
public class RestResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    public static final String REQUEST_ID_ATTRIBUTE = "requestId";

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return RestResponse.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (!(body instanceof RestResponse<?> restResponse)) {
            return body;
        }

        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
            Object requestId = httpServletRequest.getAttribute(REQUEST_ID_ATTRIBUTE);
            if (restResponse.getRequestId() == null && requestId != null) {
                restResponse.setRequestId(String.valueOf(requestId));
            }
            if (restResponse.getPath() == null) {
                restResponse.setPath(httpServletRequest.getRequestURI());
            }
        }

        if (restResponse.getTimestamp() == null) {
            restResponse.setTimestamp(LocalDateTime.now());
        }
        if (restResponse.getError() != null && restResponse.getMessage() == null) {
            restResponse.setMessage(restResponse.getError().getMessage());
        }
        return restResponse;
    }
}
