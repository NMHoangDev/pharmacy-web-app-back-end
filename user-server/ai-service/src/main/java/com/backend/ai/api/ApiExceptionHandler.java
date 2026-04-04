package com.backend.ai.api;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        log.warn("api error [{}] {} {} -> {}: {}",
                errorId,
                request.getMethod(),
                request.getRequestURI(),
                status.value(),
                ex.getReason(),
                ex);
        return ResponseEntity.status(status)
                .body(buildBody(errorId, status, request, ex.getReason(), ex));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        log.warn("api parse error [{}] {} {} -> {}",
                errorId,
                request.getMethod(),
                request.getRequestURI(),
                message,
                ex);
        return ResponseEntity.status(status)
                .body(buildBody(errorId, status, request, message, ex));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse("Validation failed");
        log.warn("api validation error [{}] {} {} -> {}",
                errorId,
                request.getMethod(),
                request.getRequestURI(),
                message,
                ex);
        return ResponseEntity.status(status)
                .body(buildBody(errorId, status, request, message, ex));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        HttpStatus status = HttpStatus.FORBIDDEN;
        String message = ex.getMessage();
        log.warn("api access denied [{}] {} {} -> {}",
                errorId,
                request.getMethod(),
                request.getRequestURI(),
                message,
                ex);
        return ResponseEntity.status(status)
                .body(buildBody(errorId, status, request, message, ex));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(
            Exception ex,
            HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        log.error("api unexpected error [{}] {} {}",
                errorId,
                request.getMethod(),
                request.getRequestURI(),
                ex);
        return ResponseEntity.status(status)
                .body(buildBody(errorId, status, request, ex.getMessage(), ex));
    }

    private Map<String, Object> buildBody(
            String errorId,
            HttpStatus status,
            HttpServletRequest request,
            String message,
            Exception ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("errorId", errorId);
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("path", request.getRequestURI());
        body.put("message", message == null || message.isBlank() ? "Unexpected error" : message);
        body.put("exception", ex.getClass().getSimpleName());
        return body;
    }
}
