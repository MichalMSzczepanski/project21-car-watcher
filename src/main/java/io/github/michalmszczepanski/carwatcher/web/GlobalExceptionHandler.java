package io.github.michalmszczepanski.carwatcher.web;

import io.github.michalmszczepanski.carwatcher.capture.ListingCaptureException;
import io.github.michalmszczepanski.carwatcher.service.EmailAlreadyRegisteredException;
import io.github.michalmszczepanski.carwatcher.service.EmailNotVerifiedException;
import io.github.michalmszczepanski.carwatcher.service.InvalidCredentialsException;
import io.github.michalmszczepanski.carwatcher.service.InvalidOrExpiredTokenException;
import io.github.michalmszczepanski.carwatcher.service.ListingNotFoundException;
import io.github.michalmszczepanski.carwatcher.web.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ListingNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ListingNotFoundException ex) {
        log.debug("404: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ListingCaptureException.class)
    public ResponseEntity<ApiError> handleCapture(ListingCaptureException ex) {
        log.warn("Błąd parsowania/przechwycenia ogłoszenia: {}", ex.getMessage(), ex);
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiError> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
        log.debug("409: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidOrExpiredTokenException.class)
    public ResponseEntity<ApiError> handleInvalidToken(InvalidOrExpiredTokenException ex) {
        log.debug("400: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.debug("401: {}", ex.getMessage());
        return error(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiError> handleEmailNotVerified(EmailNotVerifiedException ex) {
        log.debug("403: {}", ex.getMessage());
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        // Pełny ex.getMessage() wypisuje wewnętrzne nazwy klas Springa/kody walidacji —
        // zamiast tego budujemy czytelną, bezpieczną dla klienta listę "pole: komunikat".
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "Błędne dane wejściowe";
        }
        log.debug("Błąd walidacji requestu: {}", message);
        return error(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Niepoprawny/nieparsowalny JSON w body (np. brakująca wartość pola, złamana składnia) —
     * to błąd po stronie klienta, nie serwera. Bez tego handlera leciałby do {@link #handleUnexpected}
     * i dawał mylące 500 zamiast 400.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.debug("Niepoprawne body żądania (JSON): {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "Niepoprawny JSON w treści żądania");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        // To jest jedyne miejsce, które łapie WSZYSTKO, co nie zostało przewidziane wyżej —
        // bez tego loga każdy "prawdziwy" błąd 500 byłby niewidoczny w logach serwera.
        log.error("Nieoczekiwany błąd przy obsłudze żądania: {}", ex.getMessage(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Nieoczekiwany błąd serwera");
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), message));
    }
}
