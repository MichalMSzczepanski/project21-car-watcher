package io.github.michalmszczepanski.carwatcher.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AddSnapshotRequest(@NotBlank String html) {
}
