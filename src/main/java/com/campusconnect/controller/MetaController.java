package com.campusconnect.controller;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusconnect.dto.MetaResponse;
import com.campusconnect.dto.OptionResponse;
import com.campusconnect.entity.ComplaintCategory;
import com.campusconnect.entity.ComplaintStatus;
import com.campusconnect.repository.UserRepository;

/** Public endpoints: health check (used by Render) and the dropdown values for the frontend. */
@RestController
public class MetaController {

    @Autowired
    private UserRepository userRepo;

    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of("service", "CampusConnect API", "health", "/api/health");
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("database", databaseStatus());
        body.put("timestamp", Instant.now());
        return body;
    }

    @GetMapping("/api/meta")
    public MetaResponse meta() {
        return new MetaResponse(
                Arrays.stream(ComplaintCategory.values())
                        .map(c -> new OptionResponse(c.name(), c.getLabel()))
                        .toList(),
                Arrays.stream(ComplaintStatus.values())
                        .map(s -> new OptionResponse(s.name(), s.getLabel()))
                        .toList());
    }

    private String databaseStatus() {
        try {
            userRepo.count();
            return "UP";
        } catch (RuntimeException ex) {
            return "DOWN";
        }
    }
}
