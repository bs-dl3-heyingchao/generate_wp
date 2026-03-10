package com.neusoft.bsdl.wptool.api.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.neusoft.bsdl.wptool.api.dto.ApiResponse;
import com.neusoft.bsdl.wptool.core.exception.WPParseExcelException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(WPParseExcelException.class)
    public ResponseEntity<ApiResponse<Void>> handleWPParseException(WPParseExcelException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(ResponseStatusException exception) {
        int code = exception.getStatusCode().value();
        String message = exception.getReason() == null ? "Request failed" : exception.getReason();
        return ResponseEntity.status(code).body(ApiResponse.error(code, message));
    }

    @ExceptionHandler({ MissingServletRequestPartException.class, MissingServletRequestParameterException.class })
    public ResponseEntity<ApiResponse<Void>> handleBadRequestExceptions(Exception exception) {
        return ResponseEntity.badRequest().body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ApiResponse<Void>> handleErrorResponseException(ErrorResponseException exception) {
        int code = exception.getStatusCode().value();
        String message = exception.getMessage() == null ? "Request failed" : exception.getMessage();
        return ResponseEntity.status(code).body(ApiResponse.error(code, message));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unhandled exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error"));
    }
}