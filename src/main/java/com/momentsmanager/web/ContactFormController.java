package com.momentsmanager.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for handling contact form submissions from the public website
 */
@Controller
@RequestMapping("/contact")
public class ContactFormController {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    private static final String CONTACT_EMAIL = "anil.hemnani@gmail.com";

    /**
     * Handle contact form submission
     */
    @PostMapping("/submit")
    public String submitContactForm(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String phoneNumber,
            @RequestParam String weddingDate,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String guestCount,
            @RequestParam String message,
            RedirectAttributes redirectAttributes) {

        try {
            // Send email if mail sender is configured
            if (mailSender != null) {
                SimpleMailMessage mailMessage = new SimpleMailMessage();
                mailMessage.setTo(CONTACT_EMAIL);
                mailMessage.setFrom(email);
                mailMessage.setSubject("New Wedding Event Inquiry - " + fullName);

                StringBuilder emailBody = new StringBuilder();
                emailBody.append("New contact form submission from Moments Manager website:\n\n");
                emailBody.append("Name: ").append(fullName).append("\n");
                emailBody.append("Email: ").append(email).append("\n");
                emailBody.append("Phone: ").append(phoneNumber).append("\n");
                emailBody.append("Wedding Date: ").append(weddingDate).append("\n");
                emailBody.append("Location: ").append(location != null ? location : "Not specified").append("\n");
                emailBody.append("Expected Guests: ").append(guestCount != null ? guestCount : "Not specified").append("\n");
                emailBody.append("\nMessage:\n").append(message);

                mailMessage.setText(emailBody.toString());
                mailSender.send(mailMessage);
            }

            // Also log the inquiry (for records)
            logContactInquiry(fullName, email, phoneNumber, weddingDate, location, guestCount, message);

            redirectAttributes.addFlashAttribute("successMessage",
                "Thank you for your inquiry! We have received your message and will contact you within 24 hours.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "There was an issue submitting your form. Please try again or email us directly at " + CONTACT_EMAIL);
            System.err.println("Error submitting contact form: " + e.getMessage());
        }

        return "redirect:/#contact";
    }

    /**
     * Log contact inquiry to console/system (can be extended to save to database)
     */
    private void logContactInquiry(String fullName, String email, String phoneNumber,
                                   String weddingDate, String location, String guestCount, String message) {
        System.out.println("\n========== NEW CONTACT INQUIRY ==========");
        System.out.println("Name: " + fullName);
        System.out.println("Email: " + email);
        System.out.println("Phone: " + phoneNumber);
        System.out.println("Wedding Date: " + weddingDate);
        System.out.println("Location: " + (location != null ? location : "Not specified"));
        System.out.println("Expected Guests: " + (guestCount != null ? guestCount : "Not specified"));
        System.out.println("Message: " + message);
        System.out.println("========================================\n");
    }
}

