package hr.ht.rnd.wifiadmin.api.error;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import hr.ht.rnd.wifiadmin.api.dto.ErrorBody;
import hr.ht.rnd.wifiadmin.application.exception.CpeNotFoundException;
import hr.ht.rnd.wifiadmin.application.exception.PlatformFaultException;
import hr.ht.rnd.wifiadmin.application.exception.PlatformTimeoutException;
import hr.ht.rnd.wifiadmin.application.exception.PlatformUnavailableException;
import hr.ht.rnd.wifiadmin.application.exception.WifiValidationException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return badRequest(message, ApiErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorBody> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(cv -> {
                    String path = cv.getPropertyPath().toString();
                    String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                    return field + ": " + cv.getMessage();
                })
                .collect(Collectors.joining(", "));
        return badRequest(message, ApiErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorBody> handleNotReadable(HttpMessageNotReadableException ex) {
        String message = extractReadableMessage(ex);
        return badRequest(message, ApiErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler(WifiValidationException.class)
    public ResponseEntity<ErrorBody> handleWifiValidation(WifiValidationException ex) {
        return badRequest(ex.getMessage(), ApiErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler(CpeNotFoundException.class)
    public ResponseEntity<ErrorBody> handleCpeNotFound(CpeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorBody(ex.getMessage(), ApiErrorCode.CPE_NOT_FOUND.name()));
    }

    @ExceptionHandler(PlatformUnavailableException.class)
    public ResponseEntity<ErrorBody> handlePlatformUnavailable(PlatformUnavailableException ex) {
        log.warn("Platform unavailable: {}", ex.getMessage());
        return badGateway(ex.getMessage(), ApiErrorCode.PLATFORM_UNAVAILABLE);
    }

    @ExceptionHandler(PlatformFaultException.class)
    public ResponseEntity<ErrorBody> handlePlatformFault(PlatformFaultException ex) {
        log.warn("Platform fault: {}", ex.getMessage());
        return badGateway(ex.getMessage(), ApiErrorCode.PLATFORM_FAULT);
    }

    @ExceptionHandler(PlatformTimeoutException.class)
    public ResponseEntity<ErrorBody> handlePlatformTimeout(PlatformTimeoutException ex) {
        log.warn("Platform timeout: {}", ex.getMessage());
        return badGateway("Platform request timed out", ApiErrorCode.PLATFORM_TIMEOUT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError()
                .body(new ErrorBody("An unexpected error occurred", ApiErrorCode.INTERNAL_ERROR.name()));
    }

    private ResponseEntity<ErrorBody> badRequest(String message, ApiErrorCode code) {
        return ResponseEntity.badRequest().body(new ErrorBody(message, code.name()));
    }

    private ResponseEntity<ErrorBody> badGateway(String message, ApiErrorCode code) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ErrorBody(message, code.name()));
    }

    private String extractReadableMessage(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException ife) {
            String fieldPath = ife.getPath().stream()
                    .map(ref -> ref.getFieldName())
                    .collect(Collectors.joining("."));
            return "Invalid value '" + ife.getValue() + "' for field '" + fieldPath + "'";
        }
        return "Malformed or unreadable JSON request body";
    }
}
