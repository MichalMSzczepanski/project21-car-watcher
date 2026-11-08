package io.github.michalmszczepanski.carreport.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Placeholder — podmienić na prawdziwego dostawcę (SMTP/SendGrid/SES) przed produkcją,
 * wymaga prawdziwych credentiali, których nie mamy w tym środowisku. Na razie loguje
 * link weryfikacyjny, żeby dało się przetestować cały flow bez realnej wysyłki e-maili.
 */
@Component
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void sendVerificationEmail(String toEmail, String verificationLink) {
        log.info("[PLACEHOLDER e-mail] Do: {} — link weryfikacyjny: {}", toEmail, verificationLink);
    }
}
