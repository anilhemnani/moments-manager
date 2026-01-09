package com.momentsmanager.web;

import com.momentsmanager.model.EventTrafficLog;
import com.momentsmanager.model.WeddingEvent;
import com.momentsmanager.repository.EventTrafficLogRepository;
import com.momentsmanager.repository.WeddingEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TrafficLoggingInterceptor implements HandlerInterceptor {

    private static final Pattern EVENT_ID_IN_PATH = Pattern.compile("/events/([0-9]+)(/.*)?");

    @Autowired
    private WeddingEventRepository eventRepository;

    @Autowired
    private EventTrafficLogRepository trafficRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        Matcher m = EVENT_ID_IN_PATH.matcher(uri);
        if (m.find()) {
            try {
                Long eventId = Long.valueOf(m.group(1));
                WeddingEvent event = eventRepository.findById(eventId).orElse(null);
                if (event != null) {
                    EventTrafficLog log = EventTrafficLog.builder()
                            .event(event)
                            .path(uri)
                            .createdAt(LocalDateTime.now())
                            .build();
                    trafficRepository.save(log);
                }
            } catch (NumberFormatException ignored) {
                // Ignore malformed event IDs
            }
        }
        return true;
    }
}
