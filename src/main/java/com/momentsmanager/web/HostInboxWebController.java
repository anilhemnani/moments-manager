package com.momentsmanager.web;

import com.momentsmanager.model.GuestMessage;
import com.momentsmanager.model.WeddingEvent;
import com.momentsmanager.service.MessageService;
import com.momentsmanager.repository.WeddingEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Web controller for host message inbox functionality
 */
@Controller
@RequestMapping("/inbox")
public class HostInboxWebController {
    private static final Logger logger = LoggerFactory.getLogger(HostInboxWebController.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    /**
     * Display inbox for a specific event
     */
    @GetMapping("/events/{eventId}")
    public String viewEventInbox(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String filter,
            Model model) {

        try {
            WeddingEvent event = weddingEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

            Pageable pageable = PageRequest.of(page, size);
            Page<GuestMessage> messages;

            // Apply filter
            if ("unread".equals(filter)) {
                List<GuestMessage> unread = messageService.getUnreadMessages(eventId);
                messages = convertListToPage(unread, pageable);
            } else if ("inbound".equals(filter)) {
                messages = messageService.getInboundMessages(eventId, pageable);
            } else {
                messages = messageService.getEventMessages(eventId, pageable);
            }

            // Group messages by guest for better UI presentation
            Map<String, Object> groupedData = groupMessagesByGuest(messages.getContent());

            model.addAttribute("event", event);
            model.addAttribute("messages", messages);
            model.addAttribute("groupedMessages", groupedData.get("grouped"));
            model.addAttribute("guestMessageCounts", groupedData.get("counts"));
            model.addAttribute("currentFilter", filter != null ? filter : "all");
            model.addAttribute("unreadCount", messageService.getUnreadCount(eventId));
            model.addAttribute("totalCount", messageService.getEventMessageStats(eventId).get("totalMessages"));
            model.addAttribute("messageStats", messageService.getEventMessageStats(eventId));

            return "inbox/event_inbox";

        } catch (Exception e) {
            logger.error("Error viewing event inbox", e);
            model.addAttribute("error", "Error loading inbox");
            return "error";
        }
    }

    /**
     * Display all messages from a specific guest
     */
    @GetMapping("/events/{eventId}/guests/{guestId}")
    public String viewGuestConversation(
            @PathVariable Long eventId,
            @PathVariable Long guestId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Model model) {

        try {
            WeddingEvent event = weddingEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

            Pageable pageable = PageRequest.of(page, size);
            Page<GuestMessage> messages = messageService.getGuestMessages(eventId, guestId, pageable);

            // Mark all as read when viewing conversation
            messageService.markGuestMessagesAsRead(eventId, guestId);

            model.addAttribute("event", event);
            model.addAttribute("guestId", guestId);
            model.addAttribute("messages", messages);
            model.addAttribute("messageCount", messages.getTotalElements());
            model.addAttribute("conversation", messages.getContent());

            return "inbox/conversation";

        } catch (Exception e) {
            logger.error("Error viewing guest conversation", e);
            model.addAttribute("error", "Error loading conversation");
            return "error";
        }
    }

    /**
     * View single message detail
     */
    @GetMapping("/messages/{messageId}")
    public String viewMessage(
            @PathVariable Long messageId,
            Model model) {

        try {
            GuestMessage message = messageService.getMessage(messageId);

            // Mark as read
            if (!message.isRead()) {
                messageService.markAsRead(messageId);
                message = messageService.getMessage(messageId);
            }

            // Get full conversation context
            List<GuestMessage> conversation = messageService.getConversation(
                message.getEvent().getId(),
                message.getGuest().getId()
            );

            model.addAttribute("message", message);
            model.addAttribute("event", message.getEvent());
            model.addAttribute("guest", message.getGuest());
            model.addAttribute("conversation", conversation);

            return "inbox/message_detail";

        } catch (Exception e) {
            logger.error("Error viewing message", e);
            model.addAttribute("error", "Error loading message");
            return "error";
        }
    }

    /**
     * Mark message as read (AJAX)
     */
    @PostMapping("/messages/{messageId}/mark-read")
    @ResponseBody
    public Map<String, Object> markMessageAsRead(@PathVariable Long messageId) {
        try {
            messageService.markAsRead(messageId);
            return Map.of("success", true, "message", "Message marked as read");
        } catch (Exception e) {
            logger.error("Error marking message as read", e);
            return Map.of("success", false, "error", "Failed to mark message as read");
        }
    }

    /**
     * Mark message as unread (AJAX)
     */
    @PostMapping("/messages/{messageId}/mark-unread")
    @ResponseBody
    public Map<String, Object> markMessageAsUnread(@PathVariable Long messageId) {
        try {
            messageService.markAsUnread(messageId);
            return Map.of("success", true, "message", "Message marked as unread");
        } catch (Exception e) {
            logger.error("Error marking message as unread", e);
            return Map.of("success", false, "error", "Failed to mark message as unread");
        }
    }

    /**
     * Mark all messages in event as read
     */
    @PostMapping("/events/{eventId}/mark-all-read")
    @ResponseBody
    public Map<String, Object> markAllAsRead(@PathVariable Long eventId) {
        try {
            messageService.markEventMessagesAsRead(eventId);
            return Map.of("success", true, "message", "All messages marked as read");
        } catch (Exception e) {
            logger.error("Error marking all messages as read", e);
            return Map.of("success", false, "error", "Failed to mark messages as read");
        }
    }

    /**
     * Delete message
     */
    @PostMapping("/messages/{messageId}/delete")
    public String deleteMessage(
            @PathVariable Long messageId,
            RedirectAttributes redirectAttributes) {

        try {
            GuestMessage message = messageService.getMessage(messageId);
            Long eventId = message.getEvent().getId();

            messageService.deleteMessage(messageId);
            redirectAttributes.addFlashAttribute("success", "Message deleted successfully");

            return "redirect:/inbox/events/" + eventId;

        } catch (Exception e) {
            logger.error("Error deleting message", e);
            redirectAttributes.addFlashAttribute("error", "Failed to delete message");
            return "redirect:/inbox";
        }
    }

    /**
     * Get unread message count for an event (AJAX)
     */
    @GetMapping("/events/{eventId}/unread-count")
    @ResponseBody
    public Map<String, Object> getUnreadCount(@PathVariable Long eventId) {
        try {
            long count = messageService.getUnreadCount(eventId);
            return Map.of("success", true, "unreadCount", count);
        } catch (Exception e) {
            logger.error("Error getting unread count", e);
            return Map.of("success", false, "error", "Failed to get unread count");
        }
    }

    /**
     * Get message statistics for an event
     */
    @GetMapping("/events/{eventId}/stats")
    @ResponseBody
    public Map<String, Object> getStats(@PathVariable Long eventId) {
        try {
            Map<String, Object> stats = messageService.getEventMessageStats(eventId);
            stats.put("success", true);
            return stats;
        } catch (Exception e) {
            logger.error("Error getting message statistics", e);
            return Map.of("success", false, "error", "Failed to get statistics");
        }
    }

    /**
     * Helper method to group messages by guest
     */
    private Map<String, Object> groupMessagesByGuest(List<GuestMessage> messages) {
        Map<String, List<GuestMessage>> grouped = messages.stream()
            .filter(m -> m.getGuest() != null)
            .collect(Collectors.groupingBy(
                m -> m.getGuest().getFamilyName() != null ? m.getGuest().getFamilyName() : "Unknown",
                Collectors.toList()
            ));

        // Get unread counts per guest
        Map<Long, Long> unreadCounts = new HashMap<>();
        for (GuestMessage message : messages) {
            if (message.getGuest() != null && !message.isRead()) {
                Long guestId = message.getGuest().getId();
                unreadCounts.put(guestId, unreadCounts.getOrDefault(guestId, 0L) + 1);
            }
        }

        return Map.of(
            "grouped", grouped,
            "counts", unreadCounts
        );
    }

    /**
     * Convert list to Page object (helper method for pagination)
     */
    private Page<GuestMessage> convertListToPage(List<GuestMessage> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());

        List<GuestMessage> pageContent = list.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, list.size());
    }
}

