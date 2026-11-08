package io.github.michalmszczepanski.carreport.service;

public class ListingNotFoundException extends RuntimeException {

    public ListingNotFoundException(Long id) {
        super("Nie znaleziono ogłoszenia o id=" + id);
    }
}
