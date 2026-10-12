package com.campusconnect.dto;

import java.time.Instant;

import com.campusconnect.entity.Complaint;
import com.campusconnect.entity.ComplaintCategory;
import com.campusconnect.entity.ComplaintStatus;

public record ComplaintResponse(
        Long id,
        String referenceNo,
        String title,
        String description,
        ComplaintCategory category,
        String categoryLabel,
        String location,
        ComplaintStatus status,
        String statusLabel,
        String adminRemarks,
        Instant createdAt,
        Instant updatedAt,
        UserResponse student) {

    public static ComplaintResponse from(Complaint c) {
        return new ComplaintResponse(
                c.getId(),
                String.format("CC-%05d", c.getId()),
                c.getTitle(),
                c.getDescription(),
                c.getCategory(),
                c.getCategory().getLabel(),
                c.getLocation(),
                c.getStatus(),
                c.getStatus().getLabel(),
                c.getAdminRemarks(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                UserResponse.from(c.getStudent()));
    }
}
