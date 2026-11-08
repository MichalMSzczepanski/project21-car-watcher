package io.github.michalmszczepanski.carreport.web;

import io.github.michalmszczepanski.carreport.service.ListingService;
import io.github.michalmszczepanski.carreport.web.dto.AddSnapshotRequest;
import io.github.michalmszczepanski.carreport.web.dto.CaptureRequest;
import io.github.michalmszczepanski.carreport.web.dto.CaptureResponse;
import io.github.michalmszczepanski.carreport.web.dto.ListingChangeResponse;
import io.github.michalmszczepanski.carreport.web.dto.ListingResponse;
import io.github.michalmszczepanski.carreport.web.dto.ListingSummaryResponse;
import io.github.michalmszczepanski.carreport.web.dto.SnapshotResponse;
import io.github.michalmszczepanski.carreport.web.dto.UpdateListingDetailsRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/listings")
@Validated
public class ListingController {

    private static final Logger log = LoggerFactory.getLogger(ListingController.class);

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CaptureResponse> capture(@RequestBody @Valid CaptureRequest request) {
        log.debug("POST /listings url={} htmlLength={}", request.url(), length(request.html()));
        var result = listingService.capture(request.url(), request.html());
        log.debug("capture() OK listingId={} snapshotId={} newListing={} changes={}",
                result.listing().getId(), result.snapshot().getId(), result.newListing(), result.changes().size());
        return ResponseEntity.ok(CaptureResponse.from(result));
    }

    @PostMapping(path = "/{id}/snapshots", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CaptureResponse> addSnapshot(@PathVariable Long id,
                                                         @RequestBody @Valid AddSnapshotRequest request) {
        log.debug("POST /listings/{}/snapshots htmlLength={}", id, length(request.html()));
        var result = listingService.addSnapshot(id, request.html());
        log.debug("addSnapshot() OK listingId={} snapshotId={} changes={}",
                result.listing().getId(), result.snapshot().getId(), result.changes().size());
        return ResponseEntity.ok(CaptureResponse.from(result));
    }

    @GetMapping
    public ResponseEntity<List<ListingSummaryResponse>> getAllListings() {
        log.debug("GET /listings");
        List<ListingSummaryResponse> listings = listingService.getAllListingsWithLatestSnapshot().stream()
                .map(ListingSummaryResponse::from)
                .collect(Collectors.toList());
        log.debug("GET /listings -> {} wpisów", listings.size());
        return ResponseEntity.ok(listings);
    }

    @PatchMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ListingResponse> updateDetails(@PathVariable Long id,
                                                           @RequestBody @Valid UpdateListingDetailsRequest request) {
        log.debug("PATCH /listings/{} vin={} firstRegistrationDate={}", id, request.vin(), request.firstRegistrationDate());
        var listing = listingService.updateManualDetails(id, request.vin(), request.firstRegistrationDate());
        return ResponseEntity.ok(ListingResponse.from(listing));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingResponse> getListing(@PathVariable Long id) {
        log.debug("GET /listings/{}", id);
        return ResponseEntity.ok(ListingResponse.from(listingService.getListing(id)));
    }

    @GetMapping("/{id}/snapshots")
    public ResponseEntity<List<SnapshotResponse>> getSnapshots(@PathVariable Long id) {
        log.debug("GET /listings/{}/snapshots", id);
        List<SnapshotResponse> snapshots = listingService.getSnapshots(id).stream()
                .map(SnapshotResponse::from)
                .collect(Collectors.toList());
        log.debug("GET /listings/{}/snapshots -> {} wpisów", id, snapshots.size());
        return ResponseEntity.ok(snapshots);
    }

    @GetMapping("/{id}/changes")
    public ResponseEntity<List<ListingChangeResponse>> getChanges(@PathVariable Long id) {
        log.debug("GET /listings/{}/changes", id);
        List<ListingChangeResponse> changes = listingService.getChanges(id).stream()
                .map(ListingChangeResponse::from)
                .collect(Collectors.toList());
        log.debug("GET /listings/{}/changes -> {} wpisów", id, changes.size());
        return ResponseEntity.ok(changes);
    }

    private static int length(String html) {
        return html == null ? 0 : html.length();
    }
}
