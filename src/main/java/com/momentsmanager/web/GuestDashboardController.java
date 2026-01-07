package com.momentsmanager.web;

import com.momentsmanager.model.Guest;
import com.momentsmanager.model.Invitation;
import com.momentsmanager.repository.GuestRepository;
import com.momentsmanager.repository.InvitationLogRepository;
import com.momentsmanager.service.InvitationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/guest")
public class GuestDashboardController {

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private InvitationLogRepository invitationLogRepository;

    @Autowired
    private InvitationLogService invitationLogService;

    /**
     * Guest dashboard - redirects to invitation if only one exists,
     * otherwise shows list of invitations
     */
    @PreAuthorize("hasRole('GUEST')")
    @GetMapping("/dashboard")
    public String guestDashboard(Model model) {
        // Get authenticated guest's phone number
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String guestIdentifier = auth.getName();

        // For now, we'll redirect to invitations page
        // Guests access invitations through /invitations endpoint
        return "redirect:/invitations";
    }
}

