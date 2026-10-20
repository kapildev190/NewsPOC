package com.example.news.web;

import com.example.news.service.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private ProblemDetail pd(HttpStatus s, String m) { return ProblemDetail.forStatusAndDetail(s, m); }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) { return pd(HttpStatus.NOT_FOUND, e.getMessage()); }

    @ExceptionHandler(IllegalStateException.class)          // invalid workflow transition, duplicates
    ProblemDetail conflict(IllegalStateException e) { return pd(HttpStatus.CONFLICT, e.getMessage()); }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integrity(Exception e) { return pd(HttpStatus.CONFLICT, "Duplicate or invalid data"); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException e) {
        String msg = e.getFieldErrors().stream().map(f -> f.getField() + " " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b).orElse("Validation failed");
        return pd(HttpStatus.BAD_REQUEST, msg);
    }
}
