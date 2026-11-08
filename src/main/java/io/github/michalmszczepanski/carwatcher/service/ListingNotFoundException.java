package io.github.michalmszczepanski.carwatcher.service;

public class ListingNotFoundException extends RuntimeException {

    public ListingNotFoundException(Long id) {
        super("Nie znaleziono ogłoszenia o id=" + id);
    }
}
