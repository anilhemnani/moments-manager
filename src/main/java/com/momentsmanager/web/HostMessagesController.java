package com.momentsmanager.web;

import com.momentsmanager.model.GuestMessage;
import com.momentsmanager.model.WeddingEvent;
import com.momentsmanager.repository.WeddingEventRepository;
import com.momentsmanager.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for hosts to view and manage WhatsApp messages from guests
 */
@Controller
@RequestMapping("/host")
public class HostMessagesController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    /**
     * View all WhatsApp messages for host's events
     */
    @PreAuthorize("hasRole('HOST')")
    @GetMapping("/messages")
    public String viewMessages(
            @RequestParam(required = false) Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication,
            Model model) {

        // Get host's events
        String hostEmail = authentication.getName();
        List<WeddingEvent> hostEvents = weddingEventRepository.findByHostEmail(hostEmail);

        if (hostEvents.isEmpty()) {
            model.addAttribute("error", "No events found for your account");
            model.addAttribute("events", hostEvents);
            return "host/messages";
        }

        // If no event specified, use the first one
        if (eventId == null && !hostEvents.isEmpty()) {
            eventId = hostEvents.get(0).getId();
        }

        model.addAttribute("events", hostEvents);
        model.addAttribute("selectedEventId", eventId);

        if (eventId != null) {
            // Get messages for the selected event
            Pageable pageable = PageRequest.of(page, size);
            Page<GuestMessage> messagesPage = messageService.getEventMessages(eventId, pageable);

            model.addAttribute("messages", messagesPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", messagesPage.getTotalPages());
            model.addAttribute("totalMessages", messagesPage.getTotalElements());

            // Get unread count
            long unreadCount = messageService.getUnreadMessagesCount(eventId);
            model.addAttribute("unreadCount", unreadCount);
        }

        return "host/messages";
    }

    /**
     * Mark a message as read
     */
    @PreAuthorize("hasRole('HOST')")
    @PostMapping("/messages/{messageId}/mark-read")
    public String markMessageAsRead(
            @PathVariable Long messageId,
            @RequestParam(required = false) Long eventId,
            Authentication authentication) {

        messageService.markAsRead(messageId);

        String redirectUrl = "/host/messages";
        if (eventId != null) {
            redirectUrl += "?eventId=" + eventId;
        }
        return "redirect:" + redirectUrl;
    }

    /**
     * Mark all messages as read for an event
     */
    @PreAuthorize("hasRole('HOST')")
    @PostMapping("/messages/mark-all-read")
    public String markAllMessagesAsRead(
            @RequestParam Long eventId,
            Authentication authentication) {

        messageService.markAllAsReadForEvent(eventId);
        return "redirect:/host/messages?eventId=" + eventId;
    }
}

