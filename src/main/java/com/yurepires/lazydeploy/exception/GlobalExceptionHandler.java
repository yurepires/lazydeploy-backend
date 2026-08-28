package com.yurepires.lazydeploy.exception;

import com.yurepires.lazydeploy.dto.response.ErrorResponse;
import com.yurepires.lazydeploy.dto.response.FieldValidationErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

/**
 * Compatibilidade para consumidores internos que ainda invocam os handlers antigos.
 * As respostas HTTP passam a ser produzidas pelo {@link RestExceptionHandler}.
 */
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        ErrorResponse response = createErrorResponse(
                HttpStatus.NOT_FOUND,
                exception.getErrorCode(),
                exception.getMessage(),
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException exception, HttpServletRequest request) {
        ErrorResponse response = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getErrorCode(),
                exception.getMessage(),
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyRegistered(
            EmailAlreadyRegisteredException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = createErrorResponse(
                HttpStatus.CONFLICT,
                exception.getErrorCode(),
                exception.getMessage(),
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = createErrorResponse(
                HttpStatus.UNAUTHORIZED,
                exception.getErrorCode(),
                exception.getMessage(),
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(UnauthenticatedUserException.class)
    public ResponseEntity<ErrorResponse> handleUnauthenticatedUser(
            UnauthenticatedUserException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = createErrorResponse(
                HttpStatus.UNAUTHORIZED,
                exception.getErrorCode(),
                exception.getMessage(),
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationFailure(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<FieldValidationErrorResponse> validationErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new FieldValidationErrorResponse(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()
                ))
                .toList();

        ErrorResponse response = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Um ou mais campos são inválidos",
                request.getRequestURI(),
                validationErrors
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception exception, HttpServletRequest request) {
        ErrorResponse response = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "Requisição inválida",
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleRouteNotFound(NoResourceFoundException exception, HttpServletRequest request) {
        ErrorResponse response = createErrorResponse(
                HttpStatus.NOT_FOUND,
                "ROUTE_NOT_FOUND",
                "Endpoint não encontrado",
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        ErrorResponse response = createErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "METHOD_NOT_ALLOWED",
                "Método HTTP não permitido para este endpoint",
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedError(Exception exception, HttpServletRequest request) {
        log.error("Erro inesperado ao processar {}", request.getRequestURI(), exception);

        ErrorResponse response = createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "Ocorreu um erro interno inesperado",
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private ErrorResponse createErrorResponse(
            HttpStatus status,
            String code,
            String message,
            String path,
            List<FieldValidationErrorResponse> validationErrors
    ) {
        return new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                path,
                validationErrors
        );
    }
}
