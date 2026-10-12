package com.campusconnect.exception;

import java.time.Instant;
import java.util.Map;

/**
 * JSON body returned for every error, e.g.
 * { "status": 400, "error": "Bad Request", "message": "...", "fieldErrors": { "title": "..." } }
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors) {
}
