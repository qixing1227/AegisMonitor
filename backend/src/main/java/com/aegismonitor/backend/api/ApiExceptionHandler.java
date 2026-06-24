package com.aegismonitor.backend.api;

import com.aegismonitor.backend.error.AgentAccessException;
import com.aegismonitor.backend.error.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(AgentAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleAgentAccess(AgentAccessException exception) {
        HttpStatus status = exception.accessType() == AgentAccessException.AccessType.UNAUTHORIZED
            ? HttpStatus.UNAUTHORIZED
            : HttpStatus.FORBIDDEN;
        return error(status, exception.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException exception) {
        String message = exception.getReason() == null
            ? "Request failed"
            : exception.getReason();
        return error(exception.getStatusCode(), message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidArgument(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    private static ResponseEntity<ApiResponse<Void>> error(
        HttpStatusCode status,
        String message
    ) {
        HttpStatus resolvedStatus = HttpStatus.valueOf(status.value());
        return ResponseEntity
            .status(status)
            .body(new ApiResponse<>(false, resolvedStatus.name(), message, null));
    }
}