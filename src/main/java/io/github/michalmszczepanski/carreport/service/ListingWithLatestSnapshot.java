package io.github.michalmszczepanski.carreport.service;

import io.github.michalmszczepanski.carreport.domain.Listing;
import io.github.michalmszczepanski.carreport.domain.ListingSnapshot;

/**
 * Para listing + jego najnowszy snapshot (może być {@code null}, jeśli z jakiegoś powodu
 * listing nigdy nie miał udanego capture'u — w praktyce nie powinno się zdarzyć, bo
 * {@link ListingService#capture} zawsze tworzy pierwszy snapshot razem z listingiem).
 * {@code changeCount} to liczba wszystkich wykrytych zmian w historii tego listingu —
 * pod badge "coś się zmieniło" na liście, bez wchodzenia w szczegóły.
 */
public record ListingWithLatestSnapshot(Listing listing, ListingSnapshot latestSnapshot, int changeCount) {
}
