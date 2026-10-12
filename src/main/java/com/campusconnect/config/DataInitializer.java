package com.campusconnect.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.campusconnect.service.AuthService;

/**
 * Creates the administrator account on startup from ADMIN_NAME / ADMIN_EMAIL / ADMIN_PASSWORD.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private AuthService authService;

    @Value("${app.admin.name}")
    private String adminName;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        authService.ensureAdmin(adminName, adminEmail, adminPassword);
    }
}
