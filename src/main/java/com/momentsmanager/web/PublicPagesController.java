package com.momentsmanager.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

/**
 * Controller for public static pages (Privacy Policy, Terms of Service, etc.)
 * These pages are accessible without authentication
 */
@Controller
public class PublicPagesController {

    /**
     * Privacy Policy page - publicly accessible
     */
    @GetMapping("/privacy-policy")
    public String privacyPolicy() {
        return "privacy_policy";
    }

    /**
     * Icon test page - for debugging icon display issues
     */
    @GetMapping("/icon-test")
    public String iconTest() {
        return "icon_test";
    }

    /**
     * Direct icon serving endpoint - bypasses Spring Boot's static resource handling
     * This is a workaround if the standard /wedknots_icon.png path isn't working
     */
    @GetMapping(value = "/icon", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<Resource> getIcon() {
        try {
            Resource resource = new ClassPathResource("static/wedknots_icon.png");
            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Diagnostic endpoint to check static file configuration
     */
    @GetMapping("/icon-debug")
    @ResponseBody
    public String iconDebug() {
        StringBuilder debug = new StringBuilder();
        debug.append("<html><head><title>Icon Debug</title></head><body>");
        debug.append("<h1>Icon Debug Information</h1>");

        // Check if file exists
        Resource resource = new ClassPathResource("static/wedknots_icon.png");
        debug.append("<h2>File Check</h2>");
        debug.append("<p>File exists in classpath: ").append(resource.exists()).append("</p>");

        try {
            debug.append("<p>File path: ").append(resource.getURL()).append("</p>");
            debug.append("<p>File size: ").append(resource.contentLength()).append(" bytes</p>");
        } catch (IOException e) {
            debug.append("<p>Error reading file: ").append(e.getMessage()).append("</p>");
        }

        debug.append("<h2>Test Cases</h2>");
        debug.append("<h3>1. Direct Endpoint (should work)</h3>");
        debug.append("<img src='/icon' alt='Direct Endpoint' style='width:100px;border:1px solid black;'><br>");
        debug.append("<p>URL: <a href='/icon'>/icon</a></p>");

        debug.append("<h3>2. Standard Static Path</h3>");
        debug.append("<img src='/wedknots_icon.png' alt='Static Path' style='width:100px;border:1px solid black;'><br>");
        debug.append("<p>URL: <a href='/wedknots_icon.png'>/wedknots_icon.png</a></p>");

        debug.append("<h3>3. With Static Prefix</h3>");
        debug.append("<img src='/static/wedknots_icon.png' alt='Static Prefix' style='width:100px;border:1px solid black;'><br>");
        debug.append("<p>URL: <a href='/static/wedknots_icon.png'>/static/wedknots_icon.png</a></p>");

        debug.append("<h2>Browser Console</h2>");
        debug.append("<p>Open browser console (F12) and check the Network tab for 404 errors.</p>");

        debug.append("<h2>Recommendations</h2>");
        debug.append("<ul>");
        debug.append("<li>If only Test 1 works: Use /icon endpoint in your templates</li>");
        debug.append("<li>If Test 2 works: Standard path is fine, clear browser cache</li>");
        debug.append("<li>If none work: Check file exists in target/classes/static/</li>");
        debug.append("</ul>");

        debug.append("</body></html>");
        return debug.toString();
    }
}

