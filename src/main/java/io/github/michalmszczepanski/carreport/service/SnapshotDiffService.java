package io.github.michalmszczepanski.carreport.service;

import io.github.michalmszczepanski.carreport.domain.ListingChange;
import io.github.michalmszczepanski.carreport.domain.ListingSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Czysta logika porównania dwóch snapshotów, pole po polu. Bez zapisu do bazy —
 * zapis i uzupełnienie kluczy obcych (listingId/previousSnapshotId/currentSnapshotId/detectedAt)
 * robi wywołujący ({@link ListingService}).
 */
@Component
public class SnapshotDiffService {

    public List<ListingChange> diff(ListingSnapshot previous, ListingSnapshot current) {
        List<ListingChange> changes = new ArrayList<>();

        compareField(changes, "price", previous.getPrice(), current.getPrice());
        compareField(changes, "currency", previous.getCurrency(), current.getCurrency());
        compareField(changes, "mileageKm", previous.getMileageKm(), current.getMileageKm());
        compareField(changes, "make", previous.getMake(), current.getMake());
        compareField(changes, "model", previous.getModel(), current.getModel());
        compareField(changes, "version", previous.getVersion(), current.getVersion());
        compareField(changes, "productionYear", previous.getProductionYear(), current.getProductionYear());
        compareField(changes, "fuelType", previous.getFuelType(), current.getFuelType());
        compareField(changes, "gearbox", previous.getGearbox(), current.getGearbox());
        compareField(changes, "title", previous.getTitle(), current.getTitle());
        compareField(changes, "description", previous.getDescription(), current.getDescription());
        compareField(changes, "location", previous.getLocation(), current.getLocation());
        comparePhotos(changes, previous.getPhotos(), current.getPhotos());

        return changes;
    }

    private void compareField(List<ListingChange> changes, String fieldName, Object oldValue, Object newValue) {
        if (valuesEqual(oldValue, newValue)) {
            return;
        }
        ListingChange change = new ListingChange();
        change.setFieldName(fieldName);
        change.setOldValue(toComparableString(oldValue));
        change.setNewValue(toComparableString(newValue));
        changes.add(change);
    }

    /**
     * {@link BigDecimal#equals} jest wrażliwy na skalę (np. "45000" != "45000.00"), a cena
     * odczytana z bazy (previous) i świeżo sparsowana (current) mogą mieć różną skalę mimo
     * tej samej wartości — stąd osobne porównanie przez compareTo, żeby uniknąć fałszywych
     * zmian w diffie.
     */
    private boolean valuesEqual(Object oldValue, Object newValue) {
        if (oldValue instanceof BigDecimal oldDecimal && newValue instanceof BigDecimal newDecimal) {
            return oldDecimal.compareTo(newDecimal) == 0;
        }
        return Objects.equals(oldValue, newValue);
    }

    private String toComparableString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return value.toString();
    }

    private void comparePhotos(List<ListingChange> changes, List<String> oldPhotos, List<String> newPhotos) {
        Set<String> oldSet = new HashSet<>(oldPhotos == null ? List.of() : oldPhotos);
        Set<String> newSet = new HashSet<>(newPhotos == null ? List.of() : newPhotos);
        if (oldSet.equals(newSet)) {
            return;
        }
        ListingChange change = new ListingChange();
        change.setFieldName("photos");
        change.setOldValue(String.valueOf(oldSet.size()));
        change.setNewValue(String.valueOf(newSet.size()));
        changes.add(change);
    }
}
