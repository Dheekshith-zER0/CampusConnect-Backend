package com.campusconnect.dto;

import java.time.Instant;

import com.campusconnect.entity.ComplaintStatus;
import com.campusconnect.entity.StatusUpdate;

public record StatusUpdateResponse(
        Long id,
        ComplaintStatus status,
        String statusLabel,
        String remarks,
        String updatedBy,
        Instant createdAt) {

    public static StatusUpdateResponse from(StatusUpdate u) {
        return new StatusUpdateResponse(u.getId(), u.getStatus(), u.getStatus().getLabel(),
                u.getRemarks(), u.getUpdatedBy(), u.getCreatedAt());
    }
}
