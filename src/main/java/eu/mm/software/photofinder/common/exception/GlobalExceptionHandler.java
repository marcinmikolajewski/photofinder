package eu.mm.software.photofinder.common.exception;

import eu.mm.software.photofinder.user.domain.UserNotFoundException;
import eu.mm.software.photofinder.user.interfaces.rest.UserValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.net.URI;
import java.time.Instant;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleClientAbort(AsyncRequestNotUsableException ex) {

        log.debug("Client disconnected before response was fully sent: {}",
                ex.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ProblemDetail> handleSecurityException(SecurityException ex) {
        log.warn("Security violation: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Access denied"
        );
        problem.setTitle("Security Violation");
        problem.setType(URI.create("https://api.photofinder.eu/errors/security"));
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "You don't have permission to access this resource"
        );
        problem.setTitle("Access Denied");
        problem.setType(URI.create("https://api.photofinder.eu/errors/access-denied"));
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(AuthenticationException ex) {
        // Nieudane logowanie to normalny przypadek — WARN bez stacktrace, HTTP 401
        // (wcześniej wpadało do generycznego handlera → 500 + ERROR ze stackiem).
        log.warn("Failed authentication: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Invalid credentials"
        );
        problem.setTitle("Authentication Failed");
        problem.setType(URI.create("https://api.photofinder.eu/errors/authentication"));
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problem.setTitle("Invalid Request");
        problem.setType(URI.create("https://api.photofinder.eu/errors/invalid-argument"));
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalStateException(IllegalStateException ex) {
        log.error("Illegal state: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Service is in an invalid state"
        );
        problem.setTitle("Service Error");
        problem.setType(URI.create("https://api.photofinder.eu/errors/illegal-state"));
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    @ExceptionHandler(HttpClientErrorException.TooManyRequests.class)
    public ResponseEntity<ProblemDetail> handleTooManyRequests(HttpClientErrorException.TooManyRequests ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "AI service rate limit exceeded. Please try again later."
        );
        problem.setTitle("Rate Limit Exceeded");
        problem.setType(URI.create("https://api.photofinder.eu/errors/rate-limit"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("retryAfter", "60");

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", "60")
                .body(problem);
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<ProblemDetail> handleHttpServerError(HttpServerErrorException ex) {
        log.error("External service error: {} - {}", ex.getStatusCode(), ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                "External AI service is temporarily unavailable"
        );
        problem.setTitle("External Service Error");
        problem.setType(URI.create("https://api.photofinder.eu/errors/external-service"));
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(problem);
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ProblemDetail> handleHttpClientError(HttpClientErrorException ex) {
        log.error("External service client error: {} - {}", ex.getStatusCode(), ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                "Error communicating with external AI service"
        );
        problem.setTitle("External Service Error");
        problem.setType(URI.create("https://api.photofinder.eu/errors/external-service"));
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex) {
        // Loguj pełny stack trace dla debugowania, ale nie zwracaj go do klienta
        log.error("Unexpected error occurred", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later."
        );
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://api.photofinder.eu/errors/internal"));
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFoundException(
            UserNotFoundException ex,
            WebRequest request) {

        ProblemDetail body = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );

        body.setTitle("User not found");
        body.setInstance(URI.create(request.getDescription(false)
                .replace("uri=", "")));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(UserValidationException.class)
    public final ResponseEntity<ProblemDetail> handleUserValidationException(UserValidationException ex,
                                                                             WebRequest request) {


        ProblemDetail body = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );

        body.setTitle("Bad request");
        body.setInstance(URI.create(request.getDescription(false)
                .replace("uri=", "")));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}