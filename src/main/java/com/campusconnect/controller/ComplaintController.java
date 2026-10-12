package com.campusconnect.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusconnect.config.AuthInterceptor;
import com.campusconnect.dto.ComplaintDetailResponse;
import com.campusconnect.dto.ComplaintRequest;
import com.campusconnect.dto.ComplaintResponse;
import com.campusconnect.entity.User;
import com.campusconnect.service.ComplaintService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    @Autowired
    private ComplaintService complaintService;

    @PostMapping
    public ResponseEntity<ComplaintResponse> createComplaint(@Valid @RequestBody ComplaintRequest request,
            @RequestAttribute(AuthInterceptor.CURRENT_USER) User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(complaintService.createComplaint(request, user));
    }

    @GetMapping("/my")
    public List<ComplaintResponse> getMyComplaints(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user) {
        return complaintService.getMyComplaints(user);
    }

    // Available to the owning student and to administrators
    @GetMapping("/{id}")
    public ComplaintDetailResponse getComplaint(@PathVariable Long id,
            @RequestAttribute(AuthInterceptor.CURRENT_USER) User user) {
        return complaintService.getComplaintDetails(id, user);
    }

    @PutMapping("/{id}")
    public ComplaintResponse updateComplaint(@PathVariable Long id, @Valid @RequestBody ComplaintRequest request,
            @RequestAttribute(AuthInterceptor.CURRENT_USER) User user) {
        return complaintService.updateComplaint(id, request, user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComplaint(@PathVariable Long id,
            @RequestAttribute(AuthInterceptor.CURRENT_USER) User user) {
        complaintService.deleteComplaint(id, user);
        return ResponseEntity.noContent().build();
    }
}
