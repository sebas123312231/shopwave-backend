package com.shopwavefusion.rework.config;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopwavefusion.rework.api.v1.CommonDtos.ApiFieldError;
import com.shopwavefusion.rework.api.v1.CommonDtos.ProblemDetails;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    public ApiSecurityHandlers(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        write(response, 401, "UNAUTHENTICATED", "Authentication is required", request);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        write(response, 403, "FORBIDDEN", "You do not have permission to access this resource", request);
    }

    private void write(HttpServletResponse response, int status, String code, String detail,
                       HttpServletRequest request) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(new ProblemDetails(
                "https://shopwave.dev/problems/" + code.toLowerCase(), status == 401 ? "Unauthorized" : "Forbidden",
                status, code, detail, request.getRequestURI(), UUID.randomUUID().toString(), List.<ApiFieldError>of())));
    }
}
