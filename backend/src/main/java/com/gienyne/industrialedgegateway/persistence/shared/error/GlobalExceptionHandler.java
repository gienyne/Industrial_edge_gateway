package com.gienyne.industrialedgegateway.persistence.shared.error;

import com.gienyne.industrialedgegateway.persistence.user.PasswordChangeException;
import com.gienyne.industrialedgegateway.persistence.user.models.api.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;


@RestControllerAdvice
public class GlobalExceptionHandler{

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(ResourceNotFoundException exception){

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            new ApiErrorResponse(
                404,
                "Not Found",
                exception.getMessage(),
                Instant.now()
            )
        );
    }


    @ExceptionHandler(PasswordChangeException.class)
    public ResponseEntity<ApiErrorResponse> passwordChange(PasswordChangeException exception){

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ApiErrorResponse(
                400,
                "Bad Request",
                exception.getMessage(),
                Instant.now()
            )
        );
    }


    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> responseStatus(ResponseStatusException exception){

        int status = exception.getStatusCode().value();
        HttpStatus resolved = HttpStatus.resolve(status);
        String error = resolved != null ? resolved.getReasonPhrase() : "Error";

        return ResponseEntity.status(exception.getStatusCode()).body(
            new ApiErrorResponse(
                status,
                error,
                exception.getReason(),
                Instant.now())
        );
        
    }
}