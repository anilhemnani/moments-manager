package com.momentsmanager.service;

import com.momentsmanager.model.UnauthorizedAccessLog;
import com.momentsmanager.repository.UnauthorizedAccessLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Service
public class AccessAuditService {

    @Autowired
    private UnauthorizedAccessLogRepository repository;

    public void logUnauthorized(HttpServletRequest request, Authentication authentication, String reason) {
        String username = authentication != null ? authentication.getName() : "ANONYMOUS";
        String role = authentication != null && authentication.getAuthorities() != null
                ? authentication.getAuthorities().toString()
                : "NONE";

        UnauthorizedAccessLog log = UnauthorizedAccessLog.builder()
                .username(username)
                .role(role)
                .requestUri(request.getRequestURI())
                .httpMethod(request.getMethod())
                .clientIp(getClientIp(request))
                .reason(reason)
                .createdAt(LocalDateTime.now())
                .build();
        repository.save(log);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
