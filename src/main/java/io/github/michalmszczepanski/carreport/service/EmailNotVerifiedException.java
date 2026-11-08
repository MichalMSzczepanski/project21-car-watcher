package io.github.michalmszczepanski.carreport.service;

public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException() {
        super("E-mail nie został zweryfikowany — sprawdź skrzynkę i kliknij link weryfikacyjny");
    }
}
