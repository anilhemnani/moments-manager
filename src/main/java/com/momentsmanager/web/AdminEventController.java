package com.momentsmanager.web;

import com.momentsmanager.model.WeddingEvent;
import com.momentsmanager.repository.WeddingEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Admin controller for managing wedding events
 */
@Controller
@RequestMapping("/admin/events")
public class AdminEventController {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    /**
     * List all wedding events
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public String listEvents(Model model) {
        List<WeddingEvent> events = weddingEventRepository.findAll();
        model.addAttribute("events", events);
        return "admin_event_list";
    }

    /**
     * Show form to create new event
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/new")
    public String newEvent(Model model) {
        model.addAttribute("event", new WeddingEvent());
        return "admin_event_form";
    }

    /**
     * Create new event with subdomain validation
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/new")
    public String createEvent(@ModelAttribute WeddingEvent event,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        // Validate subdomain is provided
        if (event.getSubdomain() == null || event.getSubdomain().trim().isEmpty()) {
            model.addAttribute("event", event);
            model.addAttribute("error", "Subdomain is required");
            return "admin_event_form";
        }

        // Trim whitespace
        event.setSubdomain(event.getSubdomain().trim());

        // Check if subdomain is already taken
        if (weddingEventRepository.findBySubdomain(event.getSubdomain()).isPresent()) {
            model.addAttribute("event", event);
            model.addAttribute("error", "Subdomain is already taken. Please choose a different one.");
            return "admin_event_form";
        }

        // Validate subdomain format (alphanumeric and hyphens only, lowercase)
        if (!event.getSubdomain().matches("^[a-z0-9-]+$")) {
            model.addAttribute("event", event);
            model.addAttribute("error", "Subdomain must contain only lowercase letters, numbers, and hyphens.");
            return "admin_event_form";
        }

        weddingEventRepository.save(event);
        redirectAttributes.addFlashAttribute("successMessage",
                "Event created successfully! Public page: /public/" + event.getSubdomain());
        return "redirect:/admin/events";
    }

    /**
     * Show form to edit event
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/edit")
    public String editEvent(@PathVariable Long id, Model model) {
        Optional<WeddingEvent> eventOpt = weddingEventRepository.findById(id);
        if (eventOpt.isPresent()) {
            model.addAttribute("event", eventOpt.get());
            return "admin_event_form";
        }
        return "redirect:/admin/events";
    }

    /**
     * Update event (subdomain is immutable)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/edit")
    public String updateEvent(@PathVariable Long id, @ModelAttribute WeddingEvent event,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        Optional<WeddingEvent> existingOpt = weddingEventRepository.findById(id);
        if (existingOpt.isPresent()) {
            WeddingEvent existing = existingOpt.get();

            // Prevent subdomain modification - keep the original subdomain
            event.setId(id);
            event.setSubdomain(existing.getSubdomain());

            weddingEventRepository.save(event);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Event updated successfully! (Subdomain cannot be changed after creation)");
            return "redirect:/admin/events/" + id;
        }
        return "redirect:/admin/events";
    }

    /**
     * View event details
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public String viewEvent(@PathVariable Long id, Model model) {
        Optional<WeddingEvent> eventOpt = weddingEventRepository.findById(id);
        if (eventOpt.isPresent()) {
            model.addAttribute("event", eventOpt.get());
            return "admin_event_view";
        }
        return "redirect:/admin/events";
    }

    /**
     * Delete event
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/delete")
    public String deleteEvent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<WeddingEvent> eventOpt = weddingEventRepository.findById(id);
        if (eventOpt.isPresent()) {
            String eventName = eventOpt.get().getName();
            weddingEventRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Event '" + eventName + "' deleted successfully.");
        }
        return "redirect:/admin/events";
    }

    /**
     * WhatsApp configuration page
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/whatsapp-config")
    public String showWhatsAppConfig(@PathVariable Long id, Model model) {
        Optional<WeddingEvent> eventOpt = weddingEventRepository.findById(id);
        if (eventOpt.isPresent()) {
            model.addAttribute("event", eventOpt.get());
            return "admin_whatsapp_config";
        }
        return "redirect:/admin/events";
    }

    /**
     * Update WhatsApp configuration
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/whatsapp-config")
    public String updateWhatsAppConfig(@PathVariable Long id,
                                       @ModelAttribute WeddingEvent updatedEvent,
                                       RedirectAttributes redirectAttributes) {
        Optional<WeddingEvent> eventOpt = weddingEventRepository.findById(id);
        if (eventOpt.isPresent()) {
            WeddingEvent event = eventOpt.get();

            // Update WhatsApp configuration fields
            event.setWhatsappApiEnabled(updatedEvent.getWhatsappApiEnabled());
            event.setWhatsappPhoneNumberId(updatedEvent.getWhatsappPhoneNumberId());
            event.setWhatsappBusinessAccountId(updatedEvent.getWhatsappBusinessAccountId());
            event.setWhatsappAccessToken(updatedEvent.getWhatsappAccessToken());
            event.setWhatsappApiVersion(updatedEvent.getWhatsappApiVersion());
            event.setWhatsappVerifyToken(updatedEvent.getWhatsappVerifyToken());

            weddingEventRepository.save(event);

            redirectAttributes.addFlashAttribute("successMessage",
                "WhatsApp Cloud API configuration saved successfully!");
            return "redirect:/admin/events/" + id + "/whatsapp-config";
        }
        return "redirect:/admin/events";
    }
}

