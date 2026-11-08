package io.github.michalmszczepanski.carreport.capture;

public class ListingCaptureException extends RuntimeException {

    public ListingCaptureException(String message) {
        super(message);
    }

    public ListingCaptureException(String message, Throwable cause) {
        super(message, cause);
    }
}
