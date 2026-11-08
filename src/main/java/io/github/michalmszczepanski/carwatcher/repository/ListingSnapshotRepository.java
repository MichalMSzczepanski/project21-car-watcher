package io.github.michalmszczepanski.carwatcher.repository;

import io.github.michalmszczepanski.carwatcher.domain.ListingSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ListingSnapshotRepository extends JpaRepository<ListingSnapshot, Long> {

    Optional<ListingSnapshot> findFirstByListingIdOrderByCapturedAtDesc(Long listingId);

    List<ListingSnapshot> findByListingIdOrderByCapturedAtDesc(Long listingId);
}
