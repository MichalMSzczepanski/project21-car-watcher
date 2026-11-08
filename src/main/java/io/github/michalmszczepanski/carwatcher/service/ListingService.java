package io.github.michalmszczepanski.carwatcher.service;

import io.github.michalmszczepanski.carwatcher.capture.ListingCapturePort;
import io.github.michalmszczepanski.carwatcher.capture.ListingCapturePortResolver;
import io.github.michalmszczepanski.carwatcher.capture.ParsedListingData;
import io.github.michalmszczepanski.carwatcher.domain.Listing;
import io.github.michalmszczepanski.carwatcher.domain.ListingChange;
import io.github.michalmszczepanski.carwatcher.domain.ListingSnapshot;
import io.github.michalmszczepanski.carwatcher.repository.ListingChangeRepository;
import io.github.michalmszczepanski.carwatcher.repository.ListingRepository;
import io.github.michalmszczepanski.carwatcher.repository.ListingSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ListingService {

    private static final Logger log = LoggerFactory.getLogger(ListingService.class);

    private final ListingRepository listingRepository;
    private final ListingSnapshotRepository snapshotRepository;
    private final ListingChangeRepository changeRepository;
    private final ListingCapturePortResolver portResolver;
    private final SnapshotDiffService diffService;

    public ListingService(ListingRepository listingRepository,
                           ListingSnapshotRepository snapshotRepository,
                           ListingChangeRepository changeRepository,
                           ListingCapturePortResolver portResolver,
                           SnapshotDiffService diffService) {
        this.listingRepository = listingRepository;
        this.snapshotRepository = snapshotRepository;
        this.changeRepository = changeRepository;
        this.portResolver = portResolver;
        this.diffService = diffService;
    }

    /**
     * Upsert po URL: nowy URL -> nowy {@link Listing}; znany URL -> kolejny snapshot + diff.
     * To jest jedyne wejście potrzebne klientowi (bookmarklet docelowo) — nie trzeba znać ID.
     */
    @Transactional
    public CaptureResult capture(String url, String html) {
        log.debug("capture() start url={}", url);
        AtomicBoolean isNew = new AtomicBoolean(false);
        Listing listing = listingRepository.findByUrl(url).orElseGet(() -> {
            isNew.set(true);
            Listing created = new Listing();
            created.setUrl(url);
            created.setSource(ListingSourceDetector.detect(url));
            log.debug("capture() nowy listing dla url={} source={}", url, created.getSource());
            return listingRepository.save(created);
        });
        log.debug("capture() listingId={} isNew={}", listing.getId(), isNew.get());
        return recordSnapshot(listing, html, isNew.get());
    }

    @Transactional
    public CaptureResult addSnapshot(Long listingId, String html) {
        log.debug("addSnapshot() start listingId={}", listingId);
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));
        return recordSnapshot(listing, html, false);
    }

    private CaptureResult recordSnapshot(Listing listing, String html, boolean isNew) {
        log.debug("recordSnapshot() listingId={} source={} isNew={}", listing.getId(), listing.getSource(), isNew);

        ListingCapturePort port = portResolver.resolve(listing.getSource());
        log.debug("recordSnapshot() rozwiązany adapter={}", port.getClass().getSimpleName());

        ParsedListingData parsed = port.parse(html, listing.getUrl());
        log.debug("recordSnapshot() sparsowano: make={} model={} price={} mileageKm={} title={}",
                parsed.make(), parsed.model(), parsed.price(), parsed.mileageKm(), parsed.title());

        if (listing.getExternalId() == null && parsed.externalId() != null) {
            listing.setExternalId(parsed.externalId());
        }

        ListingSnapshot previous = isNew ? null
                : snapshotRepository.findFirstByListingIdOrderByCapturedAtDesc(listing.getId()).orElse(null);
        log.debug("recordSnapshot() poprzedni snapshot={}", previous == null ? "brak" : previous.getId());

        ListingSnapshot current = toSnapshot(listing.getId(), parsed);
        snapshotRepository.save(current);
        log.debug("recordSnapshot() zapisano nowy snapshotId={}", current.getId());

        List<ListingChange> changes = previous == null ? List.of() : diffService.diff(previous, current);
        log.debug("recordSnapshot() wykryto {} zmian: {}", changes.size(),
                changes.stream().map(ListingChange::getFieldName).toList());

        Instant now = Instant.now();
        for (ListingChange change : changes) {
            change.setListingId(listing.getId());
            change.setPreviousSnapshotId(previous.getId());
            change.setCurrentSnapshotId(current.getId());
            change.setDetectedAt(now);
        }
        if (!changes.isEmpty()) {
            changeRepository.saveAll(changes);
        }

        listing.setLastCheckedAt(now);
        listingRepository.save(listing);

        log.info("Przechwycono ogłoszenie listingId={} snapshotId={} isNew={} zmian={}",
                listing.getId(), current.getId(), isNew, changes.size());

        return new CaptureResult(listing, current, isNew, changes);
    }

    private ListingSnapshot toSnapshot(Long listingId, ParsedListingData parsed) {
        ListingSnapshot snapshot = new ListingSnapshot();
        snapshot.setListingId(listingId);
        snapshot.setCapturedAt(Instant.now());
        snapshot.setPrice(parsed.price());
        snapshot.setCurrency(parsed.currency());
        snapshot.setMileageKm(parsed.mileageKm());
        snapshot.setMake(parsed.make());
        snapshot.setModel(parsed.model());
        snapshot.setVersion(parsed.version());
        snapshot.setProductionYear(parsed.productionYear());
        snapshot.setFuelType(parsed.fuelType());
        snapshot.setGearbox(parsed.gearbox());
        snapshot.setTitle(parsed.title());
        snapshot.setDescription(parsed.description());
        snapshot.setLocation(parsed.location());
        snapshot.setPhotos(parsed.photos());
        snapshot.setContentHash(computeContentHash(parsed));
        return snapshot;
    }

    public Listing getListing(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new ListingNotFoundException(id));
    }

    /**
     * Ręczne uzupełnienie VIN-u / daty pierwszej rejestracji — niedostępnych w zapisanej
     * stronie ogłoszenia (VIN za reCAPTCHA, data zaszyfrowana w JSON-ie otomoto). Oba
     * parametry opcjonalne, aktualizowane tylko gdy podane (null = bez zmian).
     */
    @Transactional
    public Listing updateManualDetails(Long id, String vin, LocalDate firstRegistrationDate) {
        Listing listing = getListing(id);
        if (vin != null) {
            listing.setVin(vin);
        }
        if (firstRegistrationDate != null) {
            listing.setFirstRegistrationDate(firstRegistrationDate);
        }
        log.debug("updateManualDetails() listingId={} vin={} firstRegistrationDate={}",
                id, vin, firstRegistrationDate);
        return listingRepository.save(listing);
    }

    /**
     * Lista wszystkich obserwowanych ogłoszeń + ich najnowszy snapshot, pod dashboard.
     * Zakładamy jednego usera (bez autoryzacji) — świadoma decyzja na tym etapie, nie
     * filtrujemy po właścicielu, bo nie mamy jeszcze kont. Jedno zapytanie o najnowszy
     * snapshot per listing (N+1) jest OK przy tej skali danych — nie optymalizować przedwcześnie.
     */
    public List<ListingWithLatestSnapshot> getAllListingsWithLatestSnapshot() {
        return listingRepository.findAll().stream()
                .map(listing -> new ListingWithLatestSnapshot(
                        listing,
                        snapshotRepository.findFirstByListingIdOrderByCapturedAtDesc(listing.getId()).orElse(null),
                        (int) changeRepository.countByListingId(listing.getId())))
                .toList();
    }

    public List<ListingSnapshot> getSnapshots(Long listingId) {
        ensureExists(listingId);
        return snapshotRepository.findByListingIdOrderByCapturedAtDesc(listingId);
    }

    public List<ListingChange> getChanges(Long listingId) {
        ensureExists(listingId);
        return changeRepository.findByListingIdOrderByDetectedAtDesc(listingId);
    }

    private void ensureExists(Long listingId) {
        if (!listingRepository.existsById(listingId)) {
            throw new ListingNotFoundException(listingId);
        }
    }

    private static String computeContentHash(ParsedListingData parsed) {
        String canonical = String.join("|",
                str(parsed.price()), str(parsed.currency()), str(parsed.mileageKm()),
                str(parsed.make()), str(parsed.model()), str(parsed.version()),
                str(parsed.productionYear()), str(parsed.fuelType()), str(parsed.gearbox()),
                str(parsed.title()), str(parsed.description()), str(parsed.location()),
                String.join(",", sortedPhotos(parsed.photos())));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 niedostępne w JVM", e);
        }
    }

    private static String str(Object value) {
        return value == null ? "" : value.toString();
    }

    private static List<String> sortedPhotos(List<String> photos) {
        List<String> copy = new ArrayList<>(photos == null ? List.of() : photos);
        Collections.sort(copy);
        return copy;
    }
}
