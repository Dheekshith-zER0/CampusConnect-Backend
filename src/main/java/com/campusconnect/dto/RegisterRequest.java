package com.campusconnect.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 150, message = "Email must be at most 150 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 64, message = "Password must be 6 to 64 characters")
        String password,

        @NotBlank(message = "Roll number is required")
        @Size(max = 30, message = "Roll number must be at most 30 characters")
        String rollNumber,

        @NotBlank(message = "Department is required")
        @Size(max = 100, message = "Department must be at most 100 characters")
        String department) {
}
