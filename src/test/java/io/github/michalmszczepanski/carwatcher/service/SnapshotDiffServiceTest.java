package io.github.michalmszczepanski.carwatcher.service;

import io.github.michalmszczepanski.carwatcher.domain.ListingChange;
import io.github.michalmszczepanski.carwatcher.domain.ListingSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SnapshotDiffServiceTest {

    private final SnapshotDiffService diffService = new SnapshotDiffService();

    private ListingSnapshot baseSnapshot() {
        ListingSnapshot snapshot = new ListingSnapshot();
        snapshot.setPrice(new BigDecimal("45000"));
        snapshot.setCurrency("PLN");
        snapshot.setMileageKm(120000);
        snapshot.setMake("Ford");
        snapshot.setModel("Focus");
        snapshot.setVersion("1.6 TDCi Trend");
        snapshot.setProductionYear(2015);
        snapshot.setFuelType("Diesel");
        snapshot.setGearbox("Manualna");
        snapshot.setTitle("Testowy Ford Focus 1.6 TDCi");
        snapshot.setDescription("<p>Opis testowy</p>");
        snapshot.setLocation(null);
        snapshot.setPhotos(List.of("https://example.test/a.jpg", "https://example.test/b.jpg"));
        return snapshot;
    }

    @Test
    void brakZmianZwracaPustaListe() {
        ListingSnapshot previous = baseSnapshot();
        ListingSnapshot current = baseSnapshot();

        List<ListingChange> changes = diffService.diff(previous, current);

        assertThat(changes).isEmpty();
    }

    @Test
    void jednaZmienionaWartoscDajeJednaZmiane() {
        ListingSnapshot previous = baseSnapshot();
        ListingSnapshot current = baseSnapshot();
        current.setPrice(new BigDecimal("42000"));

        List<ListingChange> changes = diffService.diff(previous, current);

        assertThat(changes).hasSize(1);
        ListingChange change = changes.get(0);
        assertThat(change.getFieldName()).isEqualTo("price");
        assertThat(change.getOldValue()).isEqualTo("45000");
        assertThat(change.getNewValue()).isEqualTo("42000");
    }

    @Test
    void taSamaCenaZInnaSkalaBigDecimalNieJestZmiana() {
        // previous.price często wraca z bazy z inną skalą (np. "45000.00") niż świeżo
        // sparsowana wartość ("45000") — to nie powinno dawać fałszywej zmiany.
        ListingSnapshot previous = baseSnapshot();
        previous.setPrice(new BigDecimal("45000.00"));
        ListingSnapshot current = baseSnapshot();
        current.setPrice(new BigDecimal("45000"));

        List<ListingChange> changes = diffService.diff(previous, current);

        assertThat(changes).isEmpty();
    }

    @Test
    void wieluZmienionychPolNaraz() {
        ListingSnapshot previous = baseSnapshot();
        ListingSnapshot current = baseSnapshot();
        current.setPrice(new BigDecimal("42000"));
        current.setMileageKm(125000);
        current.setTitle("Zmieniony tytuł");

        List<ListingChange> changes = diffService.diff(previous, current);

        assertThat(changes).extracting(ListingChange::getFieldName)
                .containsExactlyInAnyOrder("price", "mileageKm", "title");
    }

    @Test
    void zmianaZWartosciNaNull() {
        ListingSnapshot previous = baseSnapshot();
        ListingSnapshot current = baseSnapshot();
        current.setLocation(null);
        previous.setLocation("Warszawa");

        List<ListingChange> changes = diffService.diff(previous, current);

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).getFieldName()).isEqualTo("location");
        assertThat(changes.get(0).getOldValue()).isEqualTo("Warszawa");
        assertThat(changes.get(0).getNewValue()).isNull();
    }

    @Test
    void zmianaZNullNaWartosc() {
        ListingSnapshot previous = baseSnapshot();
        previous.setLocation(null);
        ListingSnapshot current = baseSnapshot();
        current.setLocation("Kraków");

        List<ListingChange> changes = diffService.diff(previous, current);

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).getFieldName()).isEqualTo("location");
        assertThat(changes.get(0).getOldValue()).isNull();
        assertThat(changes.get(0).getNewValue()).isEqualTo("Kraków");
    }

    @Test
    void zmianaKolejnosciZdjecNieJestZmiana() {
        ListingSnapshot previous = baseSnapshot();
        previous.setPhotos(List.of("https://example.test/a.jpg", "https://example.test/b.jpg"));
        ListingSnapshot current = baseSnapshot();
        current.setPhotos(List.of("https://example.test/b.jpg", "https://example.test/a.jpg"));

        List<ListingChange> changes = diffService.diff(previous, current);

        assertThat(changes).isEmpty();
    }

    @Test
    void dodanieZdjeciaJestZmiana() {
        ListingSnapshot previous = baseSnapshot();
        previous.setPhotos(List.of("https://example.test/a.jpg"));
        ListingSnapshot current = baseSnapshot();
        current.setPhotos(List.of("https://example.test/a.jpg", "https://example.test/b.jpg"));

        List<ListingChange> changes = diffService.diff(previous, current);

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).getFieldName()).isEqualTo("photos");
        assertThat(changes.get(0).getOldValue()).isEqualTo("1");
        assertThat(changes.get(0).getNewValue()).isEqualTo("2");
    }
}
