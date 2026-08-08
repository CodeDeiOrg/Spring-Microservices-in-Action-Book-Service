package com.onlinelibrary.book.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ReviewException.class, BookException.class, MessageException.class, EmailMissingException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<Map<String, String>> handlePaymentRequired(PaymentException ex) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("message", ex.getMessage());
        if (ex.getOverdueBookTitle() != null) {
            body.put("overdueBookTitle", ex.getOverdueBookTitle());
            body.put("overdueBookDueDate", ex.getOverdueBookDueDate());
        }
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(body);
    }
}