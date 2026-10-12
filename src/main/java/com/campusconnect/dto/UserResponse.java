package com.campusconnect.dto;

import com.campusconnect.entity.Role;
import com.campusconnect.entity.User;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        String rollNumber,
        String department) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                user.getRollNumber(), user.getDepartment());
    }
}
