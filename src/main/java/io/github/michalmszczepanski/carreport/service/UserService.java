package io.github.michalmszczepanski.carreport.service;

import io.github.michalmszczepanski.carreport.domain.AuthProvider;
import io.github.michalmszczepanski.carreport.domain.User;
import io.github.michalmszczepanski.carreport.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final Duration VERIFICATION_TOKEN_TTL = Duration.ofHours(24);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final String appBaseUrl;

    public UserService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        EmailSender emailSender,
                        @Value("${app.base-url:http://localhost:8080}") String appBaseUrl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.appBaseUrl = appBaseUrl;
    }

    @Transactional
    public User register(String email, String rawPassword) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyRegisteredException(email);
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setEmailVerified(false);
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setVerificationTokenExpiresAt(Instant.now().plus(VERIFICATION_TOKEN_TTL));
        user = userRepository.save(user);

        String verificationLink = appBaseUrl + "/auth/verify?token=" + user.getVerificationToken();
        emailSender.sendVerificationEmail(user.getEmail(), verificationLink);
        log.debug("register() utworzono usera id={} email={}, czeka na weryfikację", user.getId(), email);

        return user;
    }

    @Transactional
    public User verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(InvalidOrExpiredTokenException::new);

        if (user.getVerificationTokenExpiresAt() == null || user.getVerificationTokenExpiresAt().isBefore(Instant.now())) {
            throw new InvalidOrExpiredTokenException();
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        log.debug("verifyEmail() zweryfikowano usera id={}", user.getId());
        return userRepository.save(user);
    }

    /**
     * Logowanie lokalne. Blokuje niezweryfikowane konta całkowicie (prostsze niż logowanie
     * z ograniczeniami — to MVP). Nie rozróżnia "zły email" od "złe hasło" w komunikacie
     * (żeby nie ujawniać, które adresy są zarejestrowane), ale to rozróżnienie zostaje
     * wewnętrznie w logach na potrzeby debugowania.
     */
    public User login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> {
            log.debug("login() nieznany email={}", email);
            return new InvalidCredentialsException();
        });

        if (user.getPasswordHash() == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            log.debug("login() złe hasło dla email={}", email);
            throw new InvalidCredentialsException();
        }

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }

        return user;
    }

    @Transactional
    public User findOrCreateGoogleUser(String email) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setEmail(email);
            user.setAuthProvider(AuthProvider.GOOGLE);
            user.setEmailVerified(true); // Google już zweryfikował e-mail po swojej stronie
            log.debug("findOrCreateGoogleUser() nowy user przez Google, email={}", email);
            return userRepository.save(user);
        });
    }
}
