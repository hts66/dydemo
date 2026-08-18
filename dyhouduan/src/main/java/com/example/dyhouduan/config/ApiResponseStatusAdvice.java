package com.example.dyhouduan.config;

import com.example.dyhouduan.dto.Response;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Keeps the JSON business code and the real HTTP status aligned. This allows
 * clients to handle expired logins centrally instead of every page guessing
 * whether a successful HTTP response actually contains an authentication error.
 */
@ControllerAdvice
public class ApiResponseStatusAdvice implements ResponseBodyAdvice<Response<?>> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return Response.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Response<?> beforeBodyWrite(Response<?> body,
                                       MethodParameter returnType,
                                       MediaType selectedContentType,
                                       Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                       ServerHttpRequest request,
                                       ServerHttpResponse response) {
        if (body != null && body.getCode() != null) {
            if (body.getCode() == 401) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
            } else if (body.getCode() == 403) {
                response.setStatusCode(HttpStatus.FORBIDDEN);
            } else if (body.getCode() == 429) {
                response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            }
        }
        return body;
    }
}
