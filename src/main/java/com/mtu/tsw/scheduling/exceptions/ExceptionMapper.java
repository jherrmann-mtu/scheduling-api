package com.mtu.tsw.scheduling.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Hold exception types, and map these exceptions to API responses.
 */
@RestControllerAdvice 
public class ExceptionMapper {
    
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiError(ApiException ex, HttpServletRequest request) {
        return new ResponseEntity<ApiErrorResponse>(new ApiErrorResponse(ex.status.value(), ex.getMessage()), ex.status);
    }

    public static record ApiErrorResponse(int status, String message) {}

    private static class ApiException extends RuntimeException {
        protected final HttpStatus status;

        protected ApiException(HttpStatus status, String message) {
            this.status = status;
            super(message);
        }

    }

    public static class InvalidAuthException extends ApiException {

        public InvalidAuthException(String message) {
            super(HttpStatus.FORBIDDEN, message);
        }
    }

}
