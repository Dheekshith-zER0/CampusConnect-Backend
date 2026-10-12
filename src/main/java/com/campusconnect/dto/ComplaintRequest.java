package com.campusconnect.dto;

import com.campusconnect.entity.ComplaintCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ComplaintRequest(
        @NotBlank(message = "Title is required")
        @Size(min = 5, max = 150, message = "Title must be 5 to 150 characters")
        String title,

        @NotBlank(message = "Description is required")
        @Size(min = 10, max = 2000, message = "Description must be 10 to 2000 characters")
        String description,

        @NotNull(message = "Category is required")
        ComplaintCategory category,

        @Size(max = 150, message = "Location must be at most 150 characters")
        String location) {
}
