package com.campusconnect.dto;

import com.campusconnect.entity.ComplaintStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StatusUpdateRequest(
        @NotNull(message = "Status is required")
        ComplaintStatus status,

        @Size(max = 500, message = "Remarks must be at most 500 characters")
        String remarks) {
}
