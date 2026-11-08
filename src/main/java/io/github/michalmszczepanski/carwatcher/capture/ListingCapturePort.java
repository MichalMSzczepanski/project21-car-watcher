package io.github.michalmszczepanski.carwatcher.capture;

import io.github.michalmszczepanski.carwatcher.domain.ListingSource;

/**
 * Port: kontrakt "sparsuj HTML ogłoszenia na dane domenowe", niezależny od konkretnego portalu.
 * Każdy portal (otomoto, docelowo OLX itd.) dostaje własny adapter implementujący ten port.
 */
public interface ListingCapturePort {

    boolean supports(ListingSource source);

    ParsedListingData parse(String html, String sourceUrl) throws ListingCaptureException;
}
