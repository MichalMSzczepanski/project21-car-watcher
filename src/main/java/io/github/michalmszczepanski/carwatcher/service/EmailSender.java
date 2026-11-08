package io.github.michalmszczepanski.carwatcher.service;

/**
 * Port: wysyłka e-maila weryfikacyjnego. Implementacja produkcyjna (SMTP/SendGrid/SES)
 * wymaga prawdziwych credentiali, których nie ma w tym środowisku — patrz {@link LoggingEmailSender}.
 */
public interface EmailSender {

    void sendVerificationEmail(String toEmail, String verificationLink);
}
