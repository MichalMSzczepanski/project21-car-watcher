package io.github.michalmszczepanski.carreport.service;

public class InvalidOrExpiredTokenException extends RuntimeException {

    public InvalidOrExpiredTokenException() {
        super("Link weryfikacyjny jest nieprawidłowy lub wygasł");
    }
}
