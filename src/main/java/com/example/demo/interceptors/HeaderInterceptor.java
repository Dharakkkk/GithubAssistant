package com.example.demo.interceptors;

import com.example.demo.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class HeaderInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    public HeaderInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String acceptHeader = request.getHeader("Accept");
        if ("application/xml".equals(acceptHeader)) {
            response.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
            response.setContentType("application/json");
            ApiResponse errorResponse = new ApiResponse(406, "Unsupported data type");
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));

            return false;
        }

        return true;
    }
}