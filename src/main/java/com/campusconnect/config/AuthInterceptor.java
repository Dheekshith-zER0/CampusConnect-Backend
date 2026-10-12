package com.campusconnect.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.campusconnect.entity.Role;
import com.campusconnect.entity.User;
import com.campusconnect.exception.ApiException;
import com.campusconnect.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Runs before every protected /api endpoint. It reads the "Authorization: Bearer <token>" header,
 * loads the logged-in user and stores it as a request attribute so controllers can use
 * {@code @RequestAttribute(AuthInterceptor.CURRENT_USER) User user}.
 * Paths under /api/admin additionally require the ADMIN role.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER = "currentUser";

    @Autowired
    private AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Let CORS pre-flight (OPTIONS) requests through - the browser sends them without the token
        if (!(handler instanceof HandlerMethod) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw ApiException.unauthorized("Please log in to continue");
        }

        User user = authService.findByToken(header.substring("Bearer ".length()).trim());
        if (user == null) {
            throw ApiException.unauthorized("Your session has expired. Please log in again");
        }

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/api/admin") && user.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Administrator access is required");
        }

        request.setAttribute(CURRENT_USER, user);
        return true;
    }
}
