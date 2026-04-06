package com.backend.appointment.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class AppointmentErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(AppointmentErrorHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        log.warn("Appointment API error status={} path={} message={}",
                status.value(),
                request != null ? request.getRequestURI() : "",
                ex.getReason(),
                ex);
        return ResponseEntity.status(status).body(errorBody(
                status,
                ex.getReason() != null ? ex.getReason() : status.getReasonPhrase(),
                request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(this::formatFieldError)
                .orElse("Validation failed");
        log.warn("Appointment API validation error path={} message={}",
                request != null ? request.getRequestURI() : "",
                message,
                ex);
        return ResponseEntity.badRequest().body(errorBody(HttpStatus.BAD_REQUEST, message, request));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            ConstraintViolationException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(
            Exception ex,
            HttpServletRequest request) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Bad request";
        log.warn("Appointment API bad request path={} message={}",
                request != null ? request.getRequestURI() : "",
                message,
                ex);
        return ResponseEntity.badRequest().body(errorBody(HttpStatus.BAD_REQUEST, message, request));
    }

    private Map<String, Object> errorBody(HttpStatus status, String message, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request != null ? request.getRequestURI() : "");
        return body;
    }

    private String formatFieldError(FieldError error) {
        if (error == null) {
            return "Validation failed";
        }
        if (error.getDefaultMessage() == null || error.getDefaultMessage().isBlank()) {
            return error.getField() + " is invalid";
        }
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
