package com.momentsmanager.web;

import com.momentsmanager.model.Guest;
import com.momentsmanager.model.Invitation;
import com.momentsmanager.model.InvitationLog;
import com.momentsmanager.model.WeddingEvent;
import com.momentsmanager.repository.GuestRepository;
import com.momentsmanager.repository.WeddingEventRepository;
import com.momentsmanager.service.InvitationLogService;
import com.momentsmanager.service.InvitationService;
import com.momentsmanager.service.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/events/{eventId}/invitations")
public class InvitationWebController {

    private static final Logger logger = LoggerFactory.getLogger(InvitationWebController.class);

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private InvitationLogService invitationLogService;

    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    @Autowired
    private GuestRepository guestRepository;

    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    @GetMapping
    public String listInvitations(@PathVariable Long eventId, Model model) {
        Optional<WeddingEvent> eventOpt = weddingEventRepository.findById(eventId);
        if (eventOpt.isEmpty()) {
            return "redirect:/events";
        }

        WeddingEvent event = eventOpt.get();
        List<Invitation> invitations = invitationService.getInvitationsByEvent(eventId);

        model.addAttribute("event", event);
        model.addAttribute("invitations", invitations);
        return "invitation_list";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    @GetMapping("/new")
    public String newInvitation(@PathVariable Long eventId, Model model) {
        Optional<WeddingEvent> eventOpt = weddingEventRepository.findById(eventId);
        if (eventOpt.isEmpty()) {
            return "redirect:/events";
        }

        WeddingEvent event = eventOpt.get();
        Invitation newInvitation = new Invitation();
        newInvitation.setMessageType("TEMPLATE");
        newInvitation.setTemplateLanguage("en_US");

        model.addAttribute("event", event);
        model.addAttribute("invitation", newInvitation);

        // Fetch available WhatsApp templates from Meta API if event has credentials configured
        var availableTemplates = whatsAppService.fetchAvailableTemplates(event);
        logger.info("Adding {} templates to model for event {}", availableTemplates.size(), eventId);
        if (!availableTemplates.isEmpty()) {
            logger.debug("First template: {}", availableTemplates.get(0));
        }
        model.addAttribute("availableTemplates", availableTemplates);

        return "invitation_form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    @PostMapping("/new")
    public String createInvitation(@PathVariable Long eventId,
                                   @ModelAttribute Invitation invitation,
                                   RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        try {
            invitationService.createInvitation(eventId, invitation, username);
            redirectAttributes.addFlashAttribute("successMessage", "Invitation created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to create invitation: " + e.getMessage());
        }

        return "redirect:/events/" + eventId + "/invitations";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    @GetMapping("/{invitationId}/edit")
    public String editInvitation(@PathVariable Long eventId,
                                  @PathVariable Long invitationId,
                                  Model model) {
        Optional<WeddingEvent> eventOpt = weddingEventRepository.findById(eventId);
        Optional<Invitation> invitationOpt = invitationService.getInvitationById(invitationId);

        if (eventOpt.isEmpty() || invitationOpt.isEmpty()) {
            return "redirect:/events/" + eventId + "/invitations";
        }

        WeddingEvent event = eventOpt.get();
        model.addAttribute("event", event);
        model.addAttribute("invitation", invitationOpt.get());

        // Fetch available WhatsApp templates from Meta API if event has credentials configured
        var availableTemplates = whatsAppService.fetchAvailableTemplates(event);
        logger.info("Adding {} templates to model for event {} (edit mode)", availableTemplates.size(), eventId);
        if (!availableTemplates.isEmpty()) {
            logger.debug("First template: {}", availableTemplates.get(0));
        }
        model.addAttribute("availableTemplates", availableTemplates);

        return "invitation_form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    @PostMapping("/{invitationId}/edit")
    public String updateInvitation(@PathVariable Long eventId,
                                    @PathVariable Long invitationId,
                                    @ModelAttribute Invitation invitation,
                                    RedirectAttributes redirectAttributes) {
        try {
            invitationService.updateInvitation(invitationId, invitation);
            redirectAttributes.addFlashAttribute("successMessage", "Invitation updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update invitation: " + e.getMessage());
        }

        return "redirect:/events/" + eventId + "/invitations";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    @PostMapping("/{invitationId}/delete")
    public String deleteInvitation(@PathVariable Long eventId,
                                    @PathVariable Long invitationId,
                                    RedirectAttributes redirectAttributes) {
        try {
            invitationService.deleteInvitation(invitationId);
            redirectAttributes.addFlashAttribute("successMessage", "Invitation deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete invitation: " + e.getMessage());
        }

        return "redirect:/events/" + eventId + "/invitations";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    @GetMapping("/{invitationId}/send")
    public String showSendInvitation(@PathVariable Long eventId,
                                      @PathVariable Long invitationId,
                                      @RequestParam(required = false) String side,
                                      Model model) {
        Optional<WeddingEvent> eventOpt = weddingEventRepository.findById(eventId);
        Optional<Invitation> invitationOpt = invitationService.getInvitationById(invitationId);

        if (eventOpt.isEmpty() || invitationOpt.isEmpty()) {
            return "redirect:/events/" + eventId + "/invitations";
        }

        WeddingEvent event = eventOpt.get();
        Invitation invitation = invitationOpt.get();

        // Get all guests for the event
        List<Guest> allGuests = guestRepository.findAll().stream()
                .filter(g -> g.getEventId().equals(eventId))
                .collect(Collectors.toList());

        // Filter by side if specified
        List<Guest> guests = allGuests;
        if (side != null && !side.isEmpty() && !side.equals("ALL")) {
            guests = allGuests.stream()
                    .filter(g -> g.getSide().equalsIgnoreCase(side))
                    .collect(Collectors.toList());
        }

        // Get invitation logs to mark already sent
        List<InvitationLog> logs = invitationLogService.getLogsByInvitation(invitationId);
        List<Long> sentGuestIds = logs.stream()
                .map(log -> log.getGuest().getId())
                .collect(Collectors.toList());

        model.addAttribute("event", event);
        model.addAttribute("invitation", invitation);
        model.addAttribute("guests", guests);
        model.addAttribute("sentGuestIds", sentGuestIds);
        model.addAttribute("selectedSide", side != null ? side : "ALL");
        return "invitation_send";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    @PostMapping("/{invitationId}/send")
    public String sendInvitation(@PathVariable Long eventId,
                                  @PathVariable Long invitationId,
                                  @RequestParam List<Long> guestIds,
                                  RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        try {
            List<InvitationLog> logs = invitationLogService.sendInvitationToGuests(invitationId, guestIds, username);

            long successCount = logs.stream().filter(log -> log.getDeliveryStatus().equals("SENT")).count();
            long failedCount = logs.stream().filter(log -> log.getDeliveryStatus().equals("FAILED")).count();

            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("Invitation sent to %d guest(s). Failed: %d", successCount, failedCount));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to send invitations: " + e.getMessage());
        }

        return "redirect:/events/" + eventId + "/invitations/" + invitationId + "/logs";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    @GetMapping("/{invitationId}/logs")
    public String viewInvitationLogs(@PathVariable Long eventId,
                                      @PathVariable Long invitationId,
                                      Model model) {
        Optional<WeddingEvent> eventOpt = weddingEventRepository.findById(eventId);
        Optional<Invitation> invitationOpt = invitationService.getInvitationById(invitationId);

        if (eventOpt.isEmpty() || invitationOpt.isEmpty()) {
            return "redirect:/events/" + eventId + "/invitations";
        }

        List<InvitationLog> logs = invitationLogService.getLogsByInvitation(invitationId);
        Long sentCount = invitationLogService.getInvitationSentCount(invitationId);
        Long deliveredCount = invitationLogService.getDeliveredCount(invitationId);
        Long failedCount = invitationLogService.getFailedCount(invitationId);

        model.addAttribute("event", eventOpt.get());
        model.addAttribute("invitation", invitationOpt.get());
        model.addAttribute("logs", logs);
        model.addAttribute("sentCount", sentCount);
        model.addAttribute("deliveredCount", deliveredCount);
        model.addAttribute("failedCount", failedCount);
        return "invitation_logs";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    @PostMapping("/{invitationId}/activate")
    public String activateInvitation(@PathVariable Long eventId,
                                      @PathVariable Long invitationId,
                                      RedirectAttributes redirectAttributes) {
        try {
            invitationService.activateInvitation(invitationId);
            redirectAttributes.addFlashAttribute("successMessage", "Invitation activated!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to activate invitation: " + e.getMessage());
        }

        return "redirect:/events/" + eventId + "/invitations";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    @PostMapping("/{invitationId}/archive")
    public String archiveInvitation(@PathVariable Long eventId,
                                     @PathVariable Long invitationId,
                                     RedirectAttributes redirectAttributes) {
        try {
            invitationService.archiveInvitation(invitationId);
            redirectAttributes.addFlashAttribute("successMessage", "Invitation archived!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to archive invitation: " + e.getMessage());
        }

        return "redirect:/events/" + eventId + "/invitations";
    }
}

