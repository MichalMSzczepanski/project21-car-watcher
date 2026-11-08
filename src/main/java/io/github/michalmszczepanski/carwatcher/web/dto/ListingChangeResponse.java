package io.github.michalmszczepanski.carwatcher.web.dto;

import io.github.michalmszczepanski.carwatcher.domain.ListingChange;

import java.time.Instant;

public record ListingChangeResponse(
        Long id,
        Long previousSnapshotId,
        Long currentSnapshotId,
        Instant detectedAt,
        String fieldName,
        String oldValue,
        String newValue
) {
    public static ListingChangeResponse from(ListingChange change) {
        return new ListingChangeResponse(
                change.getId(),
                change.getPreviousSnapshotId(),
                change.getCurrentSnapshotId(),
                change.getDetectedAt(),
                change.getFieldName(),
                change.getOldValue(),
                change.getNewValue()
        );
    }
}
