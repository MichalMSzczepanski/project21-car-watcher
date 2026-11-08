package io.github.michalmszczepanski.carreport.web.dto;

import io.github.michalmszczepanski.carreport.domain.ListingSnapshot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SnapshotResponse(
        Long id,
        Instant capturedAt,
        BigDecimal price,
        String currency,
        Integer mileageKm,
        String make,
        String model,
        String version,
        Integer productionYear,
        String fuelType,
        String gearbox,
        String title,
        String description,
        String location,
        List<String> photos,
        String contentHash
) {
    public static SnapshotResponse from(ListingSnapshot snapshot) {
        return new SnapshotResponse(
                snapshot.getId(),
                snapshot.getCapturedAt(),
                snapshot.getPrice(),
                snapshot.getCurrency(),
                snapshot.getMileageKm(),
                snapshot.getMake(),
                snapshot.getModel(),
                snapshot.getVersion(),
                snapshot.getProductionYear(),
                snapshot.getFuelType(),
                snapshot.getGearbox(),
                snapshot.getTitle(),
                snapshot.getDescription(),
                snapshot.getLocation(),
                snapshot.getPhotos(),
                snapshot.getContentHash()
        );
    }
}
