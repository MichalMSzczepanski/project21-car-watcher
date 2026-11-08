package io.github.michalmszczepanski.carreport.web.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record CaptureRequest(
        @NotBlank @URL String url,
        @NotBlank String html
) {
}
