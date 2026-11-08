package io.github.michalmszczepanski.carreport.repository;

import io.github.michalmszczepanski.carreport.domain.Listing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    Optional<Listing> findByUrl(String url);
}
