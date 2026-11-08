package io.github.michalmszczepanski.carreport.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email(message = "Niepoprawny format adresu e-mail") String email,
        @NotBlank @Size(min = 8, message = "Hasło musi mieć co najmniej 8 znaków") String password
) {
}
