package io.github.michalmszczepanski.carreport.web.dto;

import io.github.michalmszczepanski.carreport.domain.ListingChange;

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
