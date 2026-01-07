package com.momentsmanager.controller;

import com.momentsmanager.model.WeddingEvent;
import com.momentsmanager.repository.WeddingEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Public controller for accessing wedding events by subdomain.
 *
 * No authentication required - this is a public-facing endpoint.
 * Shows basic event information based on the subdomain extracted from the request Host header.
 *
 * Subdomain Extraction Strategy:
 * - Extracts subdomain from the request Host header (e.g., "pratibha-karthik.localhost" => "pratibha-karthik")
 * - Falls back to server name if Host header is not available
 * - Removes port number from hostname if present
 * - Uses the first part of the hostname as the subdomain
 */
@Controller
@RequestMapping("/public")
public class PublicEventController {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    /**
     * Public endpoint to view a wedding event by subdomain extracted from the request Host header.
     *
     * The subdomain is extracted from the incoming request's Host header.
     * For example, if accessing http://pratibha-karthik.localhost:8080/public,
     * the subdomain extracted will be "pratibha-karthik".
     *
     * The method then queries the database for an event with that subdomain and displays
     * the event information on a public-facing page without requiring authentication.
     *
     * @param request the HttpServletRequest used to read the Host header and determine the subdomain
     * @param model the Spring MVC Model object to pass event data to the view
     * @return "public/event_public_page" if the event is found, or "public/event_not_found" if not found
     */
    @GetMapping
    public String viewEventBySubdomain(HttpServletRequest request, Model model) {
        // Extract the host from the request header
        String host = request.getHeader("Host");
        if (host == null || host.isBlank()) {
            host = request.getServerName();
        }

        // Remove port number if present (e.g., "localhost:8080" -> "localhost")
        int colon = host.indexOf(':');
        if (colon != -1) {
            host = host.substring(0, colon);
        }

        // Extract subdomain from hostname
        // For "pratibha-karthik.localhost", extracts "pratibha-karthik"
        String subdomain = "";
        String[] parts = host.split("\\.");
        if (parts.length > 0) {
            subdomain = parts[0];
        }

        // Look up the wedding event by subdomain (first try host-derived subdomain)
        var event = weddingEventRepository.findBySubdomain(subdomain);

        // If not found, check for a "subdomain" request parameter as a last-resort fallback
        if (event.isEmpty()) {
            String paramSubdomain = request.getParameter("subdomain");
            if (paramSubdomain != null && !paramSubdomain.isBlank()) {
                subdomain = paramSubdomain.trim();
                event = weddingEventRepository.findBySubdomain(subdomain);
            }
        }

        if (event.isPresent()) {
            // Event found - populate model with event details
            WeddingEvent weddingEvent = event.get();
            model.addAttribute("event", weddingEvent);
            model.addAttribute("eventName", weddingEvent.getName());
            model.addAttribute("brideName", weddingEvent.getBrideName());
            model.addAttribute("groomName", weddingEvent.getGroomName());
            model.addAttribute("eventDate", weddingEvent.getDate());
            return "public/event_public_page";
        }

        // Event not found - show 404 page with requested subdomain
        model.addAttribute("subdomain", subdomain);
        return "public/event_not_found";
    }
}

