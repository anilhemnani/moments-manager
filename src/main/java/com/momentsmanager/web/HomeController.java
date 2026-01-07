package com.momentsmanager.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    public String index() {
        // Check if user is authenticated
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !(authentication.getPrincipal() instanceof String)) {

            // Redirect based on user role
            if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
                return "redirect:/admin/dashboard";
            } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_HOST"))) {
                return "redirect:/host/dashboard";
            } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_GUEST"))) {
                return "redirect:/guest/dashboard";
            }
        }

        // If not authenticated, show home page
        return "index";
    }
}


