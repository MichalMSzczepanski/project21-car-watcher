package io.github.michalmszczepanski.carwatcher.web.dto;

import io.github.michalmszczepanski.carwatcher.domain.Listing;
import io.github.michalmszczepanski.carwatcher.domain.ListingSnapshot;
import io.github.michalmszczepanski.carwatcher.domain.ListingSource;
import io.github.michalmszczepanski.carwatcher.domain.ListingStatus;
import io.github.michalmszczepanski.carwatcher.service.ListingWithLatestSnapshot;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Skrócony widok ogłoszenia + jego najnowszego snapshotu, pod listę/dashboard
 * (pełne szczegóły i historię daje {@code GET /listings/{id}} + {@code /snapshots} + {@code /changes}).
 */
public record ListingSummaryResponse(
        Long id,
        String url,
        ListingSource source,
        ListingStatus status,
        String title,
        String make,
        String model,
        String version,
        BigDecimal price,
        String currency,
        Integer mileageKm,
        String mainPhotoUrl,
        String vin,
        LocalDate firstRegistrationDate,
        Instant lastCheckedAt,
        int changeCount
) {
    public static ListingSummaryResponse from(ListingWithLatestSnapshot entry) {
        Listing listing = entry.listing();
        ListingSnapshot snapshot = entry.latestSnapshot();
        return new ListingSummaryResponse(
                listing.getId(),
                listing.getUrl(),
                listing.getSource(),
                listing.getStatus(),
                snapshot != null ? snapshot.getTitle() : null,
                snapshot != null ? snapshot.getMake() : null,
                snapshot != null ? snapshot.getModel() : null,
                snapshot != null ? snapshot.getVersion() : null,
                snapshot != null ? snapshot.getPrice() : null,
                snapshot != null ? snapshot.getCurrency() : null,
                snapshot != null ? snapshot.getMileageKm() : null,
                mainPhoto(snapshot),
                listing.getVin(),
                listing.getFirstRegistrationDate(),
                listing.getLastCheckedAt(),
                entry.changeCount()
        );
    }

    private static String mainPhoto(ListingSnapshot snapshot) {
        if (snapshot == null || snapshot.getPhotos() == null || snapshot.getPhotos().isEmpty()) {
            return null;
        }
        return snapshot.getPhotos().get(0);
    }
}
