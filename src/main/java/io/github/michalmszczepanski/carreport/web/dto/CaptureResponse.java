package io.github.michalmszczepanski.carreport.web.dto;

import io.github.michalmszczepanski.carreport.service.CaptureResult;

import java.util.List;
import java.util.stream.Collectors;

public record CaptureResponse(
        Long listingId,
        Long snapshotId,
        boolean newListing,
        List<ListingChangeResponse> changes
) {
    public static CaptureResponse from(CaptureResult result) {
        return new CaptureResponse(
                result.listing().getId(),
                result.snapshot().getId(),
                result.newListing(),
                result.changes().stream().map(ListingChangeResponse::from).collect(Collectors.toList())
        );
    }
}
