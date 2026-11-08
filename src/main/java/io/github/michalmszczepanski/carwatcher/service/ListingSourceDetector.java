package io.github.michalmszczepanski.carwatcher.service;

import io.github.michalmszczepanski.carwatcher.domain.ListingSource;

import java.net.URI;

public final class ListingSourceDetector {

    private ListingSourceDetector() {
    }

    public static ListingSource detect(String url) {
        String host = extractHost(url);
        if (host != null && host.contains("otomoto.pl")) {
            return ListingSource.OTOMOTO;
        }
        return ListingSource.OTHER;
    }

    private static String extractHost(String url) {
        try {
            return URI.create(url).getHost();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
