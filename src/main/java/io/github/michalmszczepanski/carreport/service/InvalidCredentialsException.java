package io.github.michalmszczepanski.carreport.service;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Nieprawidłowy e-mail lub hasło");
    }
}
