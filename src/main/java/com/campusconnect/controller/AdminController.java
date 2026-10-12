package com.campusconnect.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusconnect.config.AuthInterceptor;
import com.campusconnect.dto.ComplaintResponse;
import com.campusconnect.dto.DashboardStats;
import com.campusconnect.dto.StatusUpdateRequest;
import com.campusconnect.entity.ComplaintCategory;
import com.campusconnect.entity.ComplaintStatus;
import com.campusconnect.entity.User;
import com.campusconnect.service.ComplaintService;

import jakarta.validation.Valid;

/** Every endpoint here requires the ADMIN role (enforced by AuthInterceptor). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private ComplaintService complaintService;

    // e.g. GET /api/admin/complaints?status=IN_PROGRESS&category=ELECTRICITY&search=hostel
    @GetMapping("/complaints")
    public List<ComplaintResponse> getComplaints(
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) ComplaintCategory category,
            @RequestParam(required = false) String search) {
        return complaintService.searchComplaints(status, category, search);
    }

    @PatchMapping("/complaints/{id}/status")
    public ComplaintResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request,
            @RequestAttribute(AuthInterceptor.CURRENT_USER) User admin) {
        return complaintService.updateStatus(id, request, admin);
    }

    @GetMapping("/stats")
    public DashboardStats getStats() {
        return complaintService.getStats();
    }
}
