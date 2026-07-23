package com.aiot.log.exception;

import com.aiot.log.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request) {
        ErrorCode errorCode = exception.getErrorCode();
        if (errorCode.getHttpStatus().is5xxServerError()) {
            log.error(
                    "api_operation_failed event=business_error errorCode={} status={} method={} path={} traceId={} detail={}",
                    errorCode.name(),
                    errorCode.getHttpStatus().value(),
                    request.getMethod(),
                    request.getRequestURI(),
                    MDC.get("traceId"),
                    exception.getMessage(),
                    exception);
        } else {
            logRejected("business_error", errorCode, request, exception.getMessage());
        }
        return response(errorCode, exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            Exception exception,
            HttpServletRequest request) {
        String message = validationMessage(exception);
        logRejected("validation_failed", ErrorCode.REQUEST_VALIDATION_FAILED, request, message);
        return response(ErrorCode.REQUEST_VALIDATION_FAILED, message);
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleRequestParameterException(
            Exception exception,
            HttpServletRequest request) {
        logRejected(
                "parameter_invalid",
                ErrorCode.REQUEST_PARAMETER_INVALID,
                request,
                exception.getClass().getSimpleName());
        return response(ErrorCode.REQUEST_PARAMETER_INVALID);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        logRejected("body_invalid", ErrorCode.REQUEST_BODY_INVALID, request, "unreadable_request_body");
        return response(ErrorCode.REQUEST_BODY_INVALID);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request) {
        logRejected("method_not_allowed", ErrorCode.METHOD_NOT_ALLOWED, request, exception.getMethod());
        return response(ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
            NoResourceFoundException exception,
            HttpServletRequest request) {
        logRejected("resource_not_found", ErrorCode.RESOURCE_NOT_FOUND, request, "route_not_found");
        return response(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataConflict(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        logRejected("data_conflict", ErrorCode.DATA_CONFLICT, request, "database_constraint_violation");
        return response(ErrorCode.DATA_CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception exception,
            HttpServletRequest request) {
        log.error("api_unhandled_error errorCode={} status={} method={} path={} traceId={}",
                ErrorCode.INTERNAL_ERROR.name(),
                ErrorCode.INTERNAL_ERROR.getHttpStatus().value(),
                request.getMethod(),
                request.getRequestURI(),
                MDC.get("traceId"),
                exception);
        return response(ErrorCode.INTERNAL_ERROR);
    }

    private ResponseEntity<ApiResponse<Void>> response(ErrorCode errorCode) {
        return response(errorCode, errorCode.getMessage());
    }

    private void logRejected(
            String event,
            ErrorCode errorCode,
            HttpServletRequest request,
            String detail) {
        log.warn(
                "api_request_rejected event={} errorCode={} status={} method={} path={} traceId={} detail={}",
                event,
                errorCode.name(),
                errorCode.getHttpStatus().value(),
                request.getMethod(),
                request.getRequestURI(),
                MDC.get("traceId"),
                detail);
    }

    private ResponseEntity<ApiResponse<Void>> response(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.fail(
                        errorCode.getHttpStatus().value(),
                        errorCode.name(),
                        message));
    }

    private String validationMessage(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException validationException
                && !validationException.getBindingResult().getFieldErrors().isEmpty()) {
            return validationException.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        }
        if (exception instanceof BindException bindException
                && !bindException.getBindingResult().getFieldErrors().isEmpty()) {
            return bindException.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        }
        return ErrorCode.REQUEST_VALIDATION_FAILED.getMessage();
    }
}
