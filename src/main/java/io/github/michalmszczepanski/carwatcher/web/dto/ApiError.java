package io.github.michalmszczepanski.carwatcher.web.dto;

import java.time.Instant;

public record ApiError(Instant timestamp, int status, String message) {
}
