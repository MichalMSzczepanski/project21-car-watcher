package io.github.michalmszczepanski.carreport.web.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

/**
 * Ręczne uzupełnienie danych niedostępnych na stronie ogłoszenia (VIN ukryty za
 * reCAPTCHA, data pierwszej rejestracji zaszyfrowana w JSON-ie otomoto). Oba pola
 * opcjonalne — wysyłasz to, co akurat wiesz.
 */
public record UpdateListingDetailsRequest(
        @Pattern(regexp = "^[A-HJ-NPR-Z0-9]{17}$", message = "VIN musi mieć 17 znaków, bez liter I, O, Q")
        String vin,

        @PastOrPresent(message = "Data pierwszej rejestracji nie może być w przyszłości")
        LocalDate firstRegistrationDate
) {
}
