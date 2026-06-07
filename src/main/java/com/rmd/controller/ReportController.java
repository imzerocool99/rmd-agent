package com.rmd.controller;

import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.logging.Logger;

@RestController
@RequestMapping("/agent")
@CrossOrigin
public class ReportController {

    private static final Logger log = Logger.getLogger(ReportController.class.getName());

    @PostMapping("/send-report")
    public Map<String, Object> sendReport(@RequestBody Map<String, Object> payload) {
        String toEmail    = (String) payload.getOrDefault("toEmail",    "client@example.com");
        String clientName = (String) payload.getOrDefault("clientName", "Client");
        String subject    = (String) payload.getOrDefault("subject",    "RMD Advisory Report");
        String body       = (String) payload.getOrDefault("body",       "");

        // Demo: log the email that would be sent — wire real SMTP here for production
        log.info("=== RMD REPORT EMAIL ===");
        log.info("To      : " + toEmail);
        log.info("Name    : " + clientName);
        log.info("Subject : " + subject);
        log.info("Body    :\n" + body);
        log.info("=== END EMAIL ===");

        Map<String, Object> result = new HashMap<>();
        result.put("status",    "sent");
        result.put("toEmail",   toEmail);
        result.put("timestamp", new Date().toString());
        result.put("message",   "Report sent to " + clientName + " at " + toEmail);
        return result;
    }
}
