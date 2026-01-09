package com.momentsmanager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "unauthorized_access_log")
public class UnauthorizedAccessLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username")
    private String username;

    @Column(name = "role")
    private String role;

    @Column(name = "request_uri", length = 1024)
    private String requestUri;

    @Column(name = "http_method", length = 16)
    private String httpMethod;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "reason", length = 1024)
    private String reason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
