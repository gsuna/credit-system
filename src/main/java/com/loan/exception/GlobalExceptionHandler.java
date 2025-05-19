package com.loan.exception;

import com.loan.dto.response.GenericResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import javax.naming.AuthenticationException;
import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle ResourceNotFoundException (404 - Not Found)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<GenericResponse<String>> handleResourceNotFoundException(BusinessException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new GenericResponse<>(
                        null,
                        HttpStatus.NOT_FOUND.value(),
                        ex.getMessage()
                        )
        );
    }

    /**
     * Handle MethodArgumentNotValidException (400 - Bad Request)
     * This exception is thrown when validation on an argument annotated with @Valid fails.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new GenericResponse<>(
                        errors,
                        HttpStatus.BAD_REQUEST.value(),
                        "Validation failed"
                )
        );
    }

    /**
     * Handle ConstraintViolationException (400 - Bad Request)
     * This exception is thrown when a method parameter violates a constraint.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GenericResponse<Map<String, String>>> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new GenericResponse<>(
                        errors,
                        HttpStatus.BAD_REQUEST.value(),
                        "Constraint violation"
                )
        );
    }

    /**
     * Handle IllegalArgumentException (400 - Bad Request)
     * This exception is thrown when an illegal or inappropriate argument is passed to a method.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<GenericResponse<String>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new GenericResponse<>(
                        null,
                        HttpStatus.BAD_REQUEST.value(),
                        ex.getMessage()
                )
        );
    }

    /**
     * Handle AccessDeniedException (403 - Forbidden)
     * This exception is thrown when an authenticated user tries to access a resource they don't have permissions for.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GenericResponse<String>> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new GenericResponse<>(
                        null,
                        HttpStatus.FORBIDDEN.value(),
                        "Access denied"
                )
        );
    }


    /**
     * Handle AuthenticationException (401 - Unauthorized)
     * This exception is thrown when authentication fails for any reason.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<GenericResponse<String>> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new GenericResponse<>(
                        null,
                        HttpStatus.UNAUTHORIZED.value(),
                        "Authentication failed"
                )
        );
    }


    /**
     * Handle NullPointerException (500 - Internal Server Error)
     * This exception is thrown when an application attempts to use null where an object is required.
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<GenericResponse<String>> handleNullPointerException(NullPointerException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new GenericResponse<>(
                        null,
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Null pointer exception occurred"
                )
        );
    }

    /**
     * Handle RuntimeException (500 - Internal Server Error)
     * This is a catch-all for runtime exceptions not explicitly handled above.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<GenericResponse<String>> handleRuntimeException(RuntimeException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new GenericResponse<>(
                        null,
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        ex.getMessage()
                )
        );
    }

    /**
     * Handle all other exceptions (500 - Internal Server Error)
     * This is a catch-all for exceptions not explicitly handled above.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse<String>> handleAllExceptions(Exception ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new GenericResponse<>(
                        null,
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "An internal server error occurred"
                )
        );
    }
}