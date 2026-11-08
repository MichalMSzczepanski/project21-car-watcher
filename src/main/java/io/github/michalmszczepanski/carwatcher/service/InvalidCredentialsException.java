package io.github.michalmszczepanski.carwatcher.service;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Nieprawidłowy e-mail lub hasło");
    }
}
