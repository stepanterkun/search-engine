package com.github.stepanterkun.searchengine.common.exception;

import com.github.stepanterkun.searchengine.document.domain.model.DocumentNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleDocumentNotFound(
            DocumentNotFoundException ex
    ) {
        log.warn("Document not found: {}", ex.getMessage());

        ErrorResponseDto body = new ErrorResponseDto(
                "DOCUMENT_NOT_FOUND",
                ex.getMessage(),
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleEntityNotFound(
            EntityNotFoundException ex
    ) {
        log.warn("Entity not found: {}", ex.getMessage());

        ErrorResponseDto body = new ErrorResponseDto(
                "ENTITY_NOT_FOUND",
                ex.getMessage(),
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(
            MethodArgumentNotValidException ex
    ) {
        String details = ex.getBindingResult()
                                 .getFieldErrors()
                                 .stream()
                                 .map(this::formatFieldError)
                                 .collect(Collectors.joining("; "));

        ErrorResponseDto body = new ErrorResponseDto(
                "VALIDATION_ERROR",
                details,
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDto handleHandlerMethodValidationException(
            HandlerMethodValidationException ex
    ) {
        // take the 1st msg from all validation errors
        String message = ex.getAllErrors().stream()
                                 .map(MessageSourceResolvable::getDefaultMessage)
                                 .filter(Objects::nonNull)
                                 .findFirst()
                                 .orElse("Validation failure");

        return new ErrorResponseDto(
                "VALIDATION_ERROR",
                message,
                Instant.now()
        );
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected error on {} {} from {}",
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr(),
                ex);

        ErrorResponseDto body = new ErrorResponseDto(
                "INTERNAL_ERROR",
                "Unexpected error",
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
