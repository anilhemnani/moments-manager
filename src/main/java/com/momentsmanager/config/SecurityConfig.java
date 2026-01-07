package com.momentsmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            String requestUri = request.getRequestURI();
            String redirectUrl = "/login";

            // First, try to redirect based on the requested resource
            if (requestUri.startsWith("/admin")) {
                redirectUrl = "/login/admin";
            } else if (requestUri.startsWith("/host")) {
                redirectUrl = "/login/host";
            } else if (requestUri.startsWith("/guest")) {
                redirectUrl = "/login/guest";
            } else {
                // If no specific role path, check for previous role cookie
                jakarta.servlet.http.Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (jakarta.servlet.http.Cookie cookie : cookies) {
                        if ("lastUserRole".equals(cookie.getName())) {
                            String lastRole = cookie.getValue();
                            if ("ADMIN".equals(lastRole)) {
                                redirectUrl = "/login/admin";
                            } else if ("HOST".equals(lastRole)) {
                                redirectUrl = "/login/host";
                            } else if ("GUEST".equals(lastRole)) {
                                redirectUrl = "/login/guest";
                            }
                            break;
                        }
                    }
                }
            }

            response.sendRedirect(redirectUrl);
        };
    }

    @Bean
    public AuthenticationSuccessHandler customSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
                // Determine user role and set cookie
                String userRole = "GUEST";
                String redirectUrl = "/guest/dashboard";

                if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
                    userRole = "ADMIN";
                    redirectUrl = "/admin/dashboard";
                } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_HOST"))) {
                    userRole = "HOST";
                    redirectUrl = "/host/dashboard";
                }

                // Store the user's role in a cookie for future redirects
                jakarta.servlet.http.Cookie roleCooke = new jakarta.servlet.http.Cookie("lastUserRole", userRole);
                roleCooke.setPath("/");
                roleCooke.setMaxAge(30 * 24 * 60 * 60); // 30 days
                roleCooke.setHttpOnly(true);
                response.addCookie(roleCooke);

                response.sendRedirect(redirectUrl);
            }
        };
    }

    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            // Clear all cookies
            jakarta.servlet.http.Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (jakarta.servlet.http.Cookie cookie : cookies) {
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    response.addCookie(cookie);
                }
            }

            // Redirect to login
            response.sendRedirect("/login");
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login/**", "/register", "/css/**", "/js/**", "/set-password", "/set-password-host", "/public/**", "/api/whatsapp/webhook/**").permitAll()
                .requestMatchers("/h2-console/**").hasRole("ADMIN")  // Protect H2 console - admin only
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/host/**").hasRole("HOST")
                .requestMatchers("/guest/**").hasRole("GUEST")
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(customAuthenticationEntryPoint())
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED)
                .invalidSessionUrl("/")  // Redirect to home if session is invalid
                .maximumSessions(3)  // Allow up to 3 concurrent sessions per user
                .maxSessionsPreventsLogin(false)  // Don't block new logins, expire oldest session
                .expiredUrl("/")  // Redirect if session expired due to concurrent login
            )
            .formLogin(form -> form
                .loginPage("/login").permitAll()
                .successHandler(customSuccessHandler())
                .loginProcessingUrl("/do-not-use-spring-login") // disables default /login POST
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)  // Invalidate session on logout
                .deleteCookies("JSESSIONID", "lastUserRole")  // Delete session and role cookies
                .invalidateHttpSession(true)  // Invalidate session on logout
                .deleteCookies("JSESSIONID")  // Delete session cookie
                .permitAll()
            )
            .csrf(csrf -> csrf.disable());

        // Allow H2 console frames (needed for H2 web console)
        http.headers(headers -> headers
            .frameOptions(frameOptions -> frameOptions.sameOrigin())
        );

        return http.build();
    }
}
