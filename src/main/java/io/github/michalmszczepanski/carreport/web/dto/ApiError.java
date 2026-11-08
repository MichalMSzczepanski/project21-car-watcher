package io.github.michalmszczepanski.carreport.web.dto;

import java.time.Instant;

public record ApiError(Instant timestamp, int status, String message) {
}
