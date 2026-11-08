package io.github.michalmszczepanski.carwatcher.service;

public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException() {
        super("E-mail nie został zweryfikowany — sprawdź skrzynkę i kliknij link weryfikacyjny");
    }
}
