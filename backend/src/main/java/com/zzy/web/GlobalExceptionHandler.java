package com.zzy.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,Object>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(
                Map.of("error", "bad_request", "message", ex.getMessage())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,Object>> serverError(Exception ex) {
        return ResponseEntity.status(500).body(
                Map.of("error", "server_error", "message", ex.getMessage())
        );
    }
}
