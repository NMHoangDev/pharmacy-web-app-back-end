package com.backend.media.api;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        HttpStatus resolved = status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
        logger.warn("API error {} {} -> {} {}", request.getMethod(), request.getRequestURI(), resolved.value(),
                ex.getReason());
        ErrorResponse body = new ErrorResponse(Instant.now(), resolved.value(), resolved.getReasonPhrase(),
                ex.getReason(), request.getRequestURI());
        return ResponseEntity.status(resolved).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        logger.error("Unexpected API error {} {}", request.getMethod(), request.getRequestURI(), ex);
        ErrorResponse body = new ErrorResponse(Instant.now(), 500, "Internal Server Error", "Đã xảy ra lỗi hệ thống",
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    public record ErrorResponse(Instant timestamp, int status, String error, String message, String path) {
    }
}
