package io.github.michalmszczepanski.carwatcher.repository;

import io.github.michalmszczepanski.carwatcher.domain.ListingChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListingChangeRepository extends JpaRepository<ListingChange, Long> {

    List<ListingChange> findByListingIdOrderByDetectedAtDesc(Long listingId);

    long countByListingId(Long listingId);
}
