package dev.matriz.shared;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;

@RestControllerAdvice
public class ApiError {
    public static class Failure extends RuntimeException {
        public final int status;
        public final Map<String, String> fields;

        public Failure(int status, String message) {
            this(status, message, Map.of());
        }

        public Failure(int status, String message, Map<String, String> fields) {
            super(message);
            this.status = status;
            this.fields = fields;
        }
    }

    @ExceptionHandler(Failure.class)
    ResponseEntity<?> failure(Failure e) {
        return ResponseEntity.status(e.status).body(Map.of("code", "HTTP_" + e.status, "message", e.getMessage(), "fields", e.fields));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    ResponseEntity<?> invalid(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("code", "INVALID_REQUEST", "message", "Invalid request."));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    ResponseEntity<?> conflict(Exception e) {
        return ResponseEntity.status(409).body(Map.of("code", "CONFLICT", "message", "The data changed or violates a relationship. Refresh and try again."));
    }
}
