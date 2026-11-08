package io.github.michalmszczepanski.carwatcher.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "listings", uniqueConstraints = @UniqueConstraint(name = "uk_listing_url", columnNames = "url"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ListingSource source;

    @Column(name = "external_id")
    private String externalId;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ListingStatus status;

    /**
     * Niedostępne na stronie bez ujawnienia (reCAPTCHA) — user wpisuje je ręcznie,
     * jeśli chce je mieć przy ogłoszeniu. Cecha auta, nie snapshotu — nie zmienia się
     * między kolejnymi odczytami tego samego ogłoszenia.
     */
    @Column(length = 17)
    private String vin;

    @Column(name = "first_registration_date")
    private LocalDate firstRegistrationDate;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = ListingStatus.ACTIVE;
        }
    }
}
