package com.docsaas.api.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.docsaas.api.response.ApiResponse;


@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleApiException(ApiException ex) {
        return build(ex.getStatus(), ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        var fieldError = ex.getBindingResult().getFieldError();
        String msg = fieldError != null ? fieldError.getDefaultMessage() : "Invalid request";
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", msg);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body is missing or malformed");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleTooLarge(MaxUploadSizeExceededException ex) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "File is larger than the allowed upload size");
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Object>> handleMultipart(MultipartException ex) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_UPLOAD", "Upload could not be read");
    }

    @ExceptionHandler({ AccessDeniedException.class, AuthorizationDeniedException.class })
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(Exception ex) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to perform this action");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Method not allowed");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleAllExceptions(Exception ex) {
        // Spring's own request errors (wrong content type, missing parameter or part, bad path value...)
        // are the client's fault: answer with their 4xx status instead of a 500, and don't log a stack trace.
        if (ex instanceof org.springframework.web.ErrorResponse er && er.getStatusCode().is4xxClientError()) {
            HttpStatus status = HttpStatus.valueOf(er.getStatusCode().value());
            log.debug("Rejected request: {}", ex.getMessage());
            return build(status, "BAD_REQUEST", status == HttpStatus.UNSUPPORTED_MEDIA_TYPE
                    ? "Unsupported request format"
                    : "The request is not valid");
        }
        // Never expose internal messages (file paths, SQL, stack traces) to clients.
        log.error("Unhandled error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Something went wrong. Please try again.");
    }

    private ResponseEntity<ApiResponse<Object>> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(code, message));
    }
}
