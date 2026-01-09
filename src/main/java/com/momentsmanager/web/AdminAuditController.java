package com.momentsmanager.web;

import com.momentsmanager.repository.UnauthorizedAccessLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/audit")
public class AdminAuditController {

    @Autowired
    private UnauthorizedAccessLogRepository repository;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/unauthorized")
    public String viewUnauthorized(Model model) {
        model.addAttribute("logs", repository.findTop100ByOrderByCreatedAtDesc());
        return "admin_unauthorized_logs";
    }
}
