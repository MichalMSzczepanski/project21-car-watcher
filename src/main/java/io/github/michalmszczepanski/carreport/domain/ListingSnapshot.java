package io.github.michalmszczepanski.carreport.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "listing_snapshots", indexes = @Index(name = "idx_snapshot_listing", columnList = "listing_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ListingSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    private BigDecimal price;

    @Column(length = 3)
    private String currency;

    private Integer mileageKm;
    private String make;
    private String model;
    private String version;
    private Integer productionYear;
    private String fuelType;
    private String gearbox;

    @Column(length = 500)
    private String title;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String description;

    private String location;

    @Lob
    @Column(name = "photos", columnDefinition = "CLOB")
    @Convert(converter = StringListJsonConverter.class)
    private List<String> photos;

    @Column(name = "content_hash", length = 64)
    private String contentHash;
}
