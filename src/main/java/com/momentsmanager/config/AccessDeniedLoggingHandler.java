package com.momentsmanager.config;

import com.momentsmanager.service.AccessAuditService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AccessDeniedLoggingHandler implements AccessDeniedHandler {

    @Autowired
    private AccessAuditService accessAuditService;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, org.springframework.security.access.AccessDeniedException accessDeniedException) throws IOException, ServletException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        accessAuditService.logUnauthorized(request, auth, "Spring Security access denied: " + accessDeniedException.getMessage());
        response.sendRedirect("/forbidden");
    }
}
