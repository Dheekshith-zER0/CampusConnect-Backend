package com.campusconnect.service;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.campusconnect.dto.AuthResponse;
import com.campusconnect.dto.LoginRequest;
import com.campusconnect.dto.RegisterRequest;
import com.campusconnect.dto.UserResponse;
import com.campusconnect.entity.Role;
import com.campusconnect.entity.User;
import com.campusconnect.exception.ApiException;
import com.campusconnect.repository.UserRepository;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepo;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    /** Students register themselves. Administrator accounts are never created through this endpoint. */
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepo.existsByEmail(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.STUDENT);
        user.setRollNumber(request.rollNumber().trim());
        user.setDepartment(request.department().trim());
        user.setAuthToken(newToken());
        userRepo.save(user);

        return new AuthResponse(user.getAuthToken(), UserResponse.from(user));
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepo.findByEmail(normalizeEmail(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));

        // Reuse an existing token so the user can stay logged in on several devices
        if (user.getAuthToken() == null) {
            user.setAuthToken(newToken());
            userRepo.save(user);
        }
        return new AuthResponse(user.getAuthToken(), UserResponse.from(user));
    }

    public void logout(User user) {
        user.setAuthToken(null);
        userRepo.save(user);
    }

    public User findByToken(String token) {
        return userRepo.findByAuthToken(token).orElse(null);
    }

    /**
     * Makes sure the configured administrator exists and that its password matches the
     * configuration, so the admin login can always be recovered by changing ADMIN_PASSWORD.
     */
    public void ensureAdmin(String name, String email, String password) {
        String normalized = normalizeEmail(email);
        User admin = userRepo.findByEmail(normalized).orElse(null);

        if (admin == null) {
            admin = new User();
            admin.setName(name);
            admin.setEmail(normalized);
            admin.setPassword(passwordEncoder.encode(password));
            admin.setRole(Role.ADMIN);
            userRepo.save(admin);
            log.info("Created administrator account {}", normalized);
            return;
        }

        if (admin.getRole() != Role.ADMIN || !passwordEncoder.matches(password, admin.getPassword())) {
            admin.setRole(Role.ADMIN);
            admin.setPassword(passwordEncoder.encode(password));
            admin.setAuthToken(null);
            userRepo.save(admin);
            log.info("Updated administrator account {}", normalized);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
