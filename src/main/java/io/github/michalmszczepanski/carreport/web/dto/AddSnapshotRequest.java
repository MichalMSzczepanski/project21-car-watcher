package io.github.michalmszczepanski.carreport.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AddSnapshotRequest(@NotBlank String html) {
}
