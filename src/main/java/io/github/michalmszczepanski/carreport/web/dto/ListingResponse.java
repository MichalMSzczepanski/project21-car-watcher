package io.github.michalmszczepanski.carreport.web.dto;

import io.github.michalmszczepanski.carreport.domain.Listing;
import io.github.michalmszczepanski.carreport.domain.ListingSource;
import io.github.michalmszczepanski.carreport.domain.ListingStatus;

import java.time.Instant;
import java.time.LocalDate;

public record ListingResponse(
        Long id,
        ListingSource source,
        String externalId,
        String url,
        Instant createdAt,
        Instant lastCheckedAt,
        ListingStatus status,
        String vin,
        LocalDate firstRegistrationDate
) {
    public static ListingResponse from(Listing listing) {
        return new ListingResponse(
                listing.getId(),
                listing.getSource(),
                listing.getExternalId(),
                listing.getUrl(),
                listing.getCreatedAt(),
                listing.getLastCheckedAt(),
                listing.getStatus(),
                listing.getVin(),
                listing.getFirstRegistrationDate()
        );
    }
}
