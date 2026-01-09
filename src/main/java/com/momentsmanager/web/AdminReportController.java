package com.momentsmanager.web;

import com.momentsmanager.model.WeddingEvent;
import com.momentsmanager.repository.WeddingEventRepository;
import com.momentsmanager.service.AdminReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/reports")
public class AdminReportController {

    @Autowired
    private AdminReportService reportService;

    @Autowired
    private WeddingEventRepository eventRepository;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public String viewReports(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "25") int size,
            @RequestParam(name = "search", required = false) String search,
            Model model) {

        Pageable pageable = PageRequest.of(page, size);
        Page<WeddingEvent> eventPage = reportService.findEventsPaged(search, pageable);

        model.addAttribute("summary", reportService.buildSummaryForEvents(eventPage.getContent()));
        model.addAttribute("events", eventPage.getContent());
        model.addAttribute("page", eventPage);
        model.addAttribute("search", search == null ? "" : search);
        model.addAttribute("size", size);
        return "admin_reports";
    }
}
