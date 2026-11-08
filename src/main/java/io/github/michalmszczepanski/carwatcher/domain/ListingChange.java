package io.github.michalmszczepanski.carwatcher.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "listing_changes", indexes = @Index(name = "idx_change_listing", columnList = "listing_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ListingChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Column(name = "previous_snapshot_id", nullable = false)
    private Long previousSnapshotId;

    @Column(name = "current_snapshot_id", nullable = false)
    private Long currentSnapshotId;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "field_name", nullable = false, length = 50)
    private String fieldName;

    // bez @Lob to by się wysypało na VARCHAR(255) przy zmianie np. "description"
    // (opisy ogłoszeń regularnie przekraczają 255 znaków) — znalezione na żywym buggu.
    @Lob
    @Column(name = "old_value", columnDefinition = "CLOB")
    private String oldValue;

    @Lob
    @Column(name = "new_value", columnDefinition = "CLOB")
    private String newValue;
}
