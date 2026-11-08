package io.github.michalmszczepanski.carreport.capture;

import java.math.BigDecimal;
import java.util.List;

public record ParsedListingData(
        String externalId,
        String title,
        String description,
        BigDecimal price,
        String currency,
        Integer mileageKm,
        String make,
        String model,
        String version,
        Integer productionYear,
        String fuelType,
        String gearbox,
        String location,
        List<String> photos
) {
}
