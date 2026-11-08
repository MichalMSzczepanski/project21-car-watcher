package io.github.michalmszczepanski.carreport.service;

import io.github.michalmszczepanski.carreport.domain.Listing;
import io.github.michalmszczepanski.carreport.domain.ListingChange;
import io.github.michalmszczepanski.carreport.domain.ListingSnapshot;

import java.util.List;

public record CaptureResult(Listing listing, ListingSnapshot snapshot, boolean newListing, List<ListingChange> changes) {
}
