package io.github.michalmszczepanski.carreport.service;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(String email) {
        super("Konto dla adresu " + email + " już istnieje");
    }
}
