package com.yurepires.lazydeploy.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                exception.getMessage(),
                "RESOURCE_NOT_FOUND",
                request
        );
    }

    @ExceptionHandler(MapNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleMapNotFound(
            MapNotFoundException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.NOT_FOUND,
                "Map not found",
                exception.getMessage(),
                exception.getErrorCode(),
                request
        );
    }

    @ExceptionHandler(SubscriptionAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleSubscriptionConflict(
            SubscriptionAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.CONFLICT,
                "Subscription conflict",
                exception.getMessage(),
                exception.getErrorCode(),
                request
        );
    }

    @ExceptionHandler({
            ChannelAlreadyExistsException.class,
            EmailAlreadyRegisteredException.class,
            DataIntegrityViolationException.class
    })
    public ResponseEntity<ProblemDetail> handleConflict(
            Exception exception,
            HttpServletRequest request
    ) {
        String errorCode = "RESOURCE_CONFLICT";
        String detail = "A operação entra em conflito com um recurso existente";

        if (exception instanceof ApplicationException applicationException) {
            errorCode = applicationException.getErrorCode();
            detail = applicationException.getMessage();
        }

        if (exception instanceof DataIntegrityViolationException) {
            log.warn(
                    "Violação de integridade ao processar {} | exceptionType={}",
                    request.getRequestURI(),
                    exception.getClass().getSimpleName()
            );
        }

        return response(
                HttpStatus.CONFLICT,
                "Resource conflict",
                detail,
                errorCode,
                request
        );
    }

    @ExceptionHandler({
            UnsupportedRuleTypeException.class,
            InvalidRuleParametersException.class,
            UnsupportedChannelTypeException.class,
            InvalidChannelConfigurationException.class,
            UnknownMapException.class,
            DuplicateRuleTypeException.class,
            DuplicateChannelTypeException.class,
            NoActiveNotificationChannelException.class,
            SubscriptionLimitReachedException.class,
            RuleLimitReachedException.class,
            ChannelLimitReachedException.class,
            TooManyMapsException.class
    })
    public ResponseEntity<ProblemDetail> handleUnprocessableEntity(
            Exception exception,
            HttpServletRequest request
    ) {
        String errorCode = "UNPROCESSABLE_ENTITY";
        String detail = exception.getMessage();

        if (exception instanceof ApplicationException applicationException) {
            errorCode = applicationException.getErrorCode();
            detail = applicationException.getMessage();
        } else if (exception instanceof UnsupportedRuleTypeException unsupportedRuleTypeException) {
            errorCode = unsupportedRuleTypeException.getErrorCode();
        }

        return response(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Unprocessable entity",
                detail,
                errorCode,
                request
        );
    }

    @ExceptionHandler({
            InvalidCredentialsException.class,
            UnauthenticatedUserException.class
    })
    public ResponseEntity<ProblemDetail> handleAuthenticationFailure(
            ApplicationException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.UNAUTHORIZED,
                "Authentication required",
                exception.getMessage(),
                exception.getErrorCode(),
                request
        );
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimitExceeded(
            RateLimitExceededException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetailFactory.create(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                exception.getMessage(),
                exception.getErrorCode(),
                request.getRequestURI(),
                List.of()
        );
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", String.valueOf(exception.getRetryAfterSeconds()));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .headers(headers)
                .body(problemDetail);
    }

    @ExceptionHandler(ExternalProviderUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleProviderUnavailable(
            ExternalProviderUnavailableException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.SERVICE_UNAVAILABLE,
                "External provider unavailable",
                exception.getMessage(),
                exception.getErrorCode(),
                request
        );
    }

    @ExceptionHandler(ExternalProviderException.class)
    public ResponseEntity<ProblemDetail> handleProviderFailure(
            ExternalProviderException exception,
            HttpServletRequest request
    ) {
        boolean concurrencyLimitReached =
                exception.category() == ExternalProviderFailureCategory.CONCURRENCY_LIMIT_REACHED;

        if (concurrencyLimitReached) {
            return response(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "External provider busy",
                    "A busca de servidores está temporariamente ocupada. Tente novamente.",
                    "EXTERNAL_PROVIDER_BUSY",
                    request
            );
        }

        return response(
                HttpStatus.SERVICE_UNAVAILABLE,
                "External provider unavailable",
                "Não foi possível consultar os servidores agora. Tente novamente.",
                "EXTERNAL_PROVIDER_UNAVAILABLE",
                request
        );
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ProblemDetail> handleInvalidRequest(
            InvalidRequestException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                exception.getMessage(),
                exception.getErrorCode(),
                request
        );
    }

    @ExceptionHandler({
            SearchQueryTooShortException.class,
            SearchQueryTooLongException.class,
            InvalidPageException.class,
            InvalidPageSizeException.class,
            PageSizeLimitExceededException.class
    })
    public ResponseEntity<ProblemDetail> handleLimitValidation(
            ApplicationException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                exception.getMessage(),
                exception.getErrorCode(),
                request
        );
    }

    @ExceptionHandler(RequestTooLargeException.class)
    public ResponseEntity<ProblemDetail> handleRequestTooLarge(
            RequestTooLargeException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Payload Too Large",
                "O corpo da requisição excede o tamanho máximo permitido.",
                "REQUEST_TOO_LARGE",
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationFailure(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ProblemDetailFactory.FieldValidationError> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ProblemDetailFactory.FieldValidationError(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();

        return response(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                "Um ou mais campos são inválidos",
                "VALIDATION_FAILED",
                request,
                fieldErrors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMalformedJson(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                "Malformed request",
                "O corpo da requisição não pôde ser interpretado",
                "MALFORMED_REQUEST",
                request
        );
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ProblemDetail> handleOtherValidationFailure(
            Exception exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                "A requisição contém valores inválidos",
                "VALIDATION_FAILED",
                request
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleRouteNotFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                "Endpoint não encontrado",
                "RESOURCE_NOT_FOUND",
                request
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Method not allowed",
                "Método HTTP não permitido para este endpoint",
                "METHOD_NOT_ALLOWED",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedError(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Erro inesperado ao processar {} | exceptionType={}",
                request.getRequestURI(),
                exception.getClass().getSimpleName()
        );
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "Ocorreu um erro interno inesperado",
                "INTERNAL_ERROR",
                request
        );
    }

    private ResponseEntity<ProblemDetail> response(
            HttpStatus status,
            String title,
            String detail,
            String errorCode,
            HttpServletRequest request
    ) {
        return response(status, title, detail, errorCode, request, List.of());
    }

    private ResponseEntity<ProblemDetail> response(
            HttpStatus status,
            String title,
            String detail,
            String errorCode,
            HttpServletRequest request,
            List<ProblemDetailFactory.FieldValidationError> fieldErrors
    ) {
        ProblemDetail problemDetail = ProblemDetailFactory.create(
                status,
                title,
                detail,
                errorCode,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(status).body(problemDetail);
    }
}
