package io.github.michalmszczepanski.carwatcher.service;

import io.github.michalmszczepanski.carwatcher.domain.Listing;
import io.github.michalmszczepanski.carwatcher.domain.ListingChange;
import io.github.michalmszczepanski.carwatcher.domain.ListingSnapshot;

import java.util.List;

public record CaptureResult(Listing listing, ListingSnapshot snapshot, boolean newListing, List<ListingChange> changes) {
}
