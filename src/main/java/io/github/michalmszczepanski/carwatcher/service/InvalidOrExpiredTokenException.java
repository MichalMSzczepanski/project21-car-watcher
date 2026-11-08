package io.github.michalmszczepanski.carwatcher.service;

public class InvalidOrExpiredTokenException extends RuntimeException {

    public InvalidOrExpiredTokenException() {
        super("Link weryfikacyjny jest nieprawidłowy lub wygasł");
    }
}
