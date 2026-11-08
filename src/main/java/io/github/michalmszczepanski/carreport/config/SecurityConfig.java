package io.github.michalmszczepanski.carreport.config;

import io.github.michalmszczepanski.carreport.web.GoogleLoginSuccessHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.List;

/**
 * Większość tego projektu (REST API pod /listings, statyczny frontend) ma zostać w pełni
 * otwarta — to świadoma decyzja z obecnego etapu (jeden zakładany user, bez autoryzacji
 * per-zasób). Ten config dodaje TYLKO konta/logowanie jako nowy, równoległy podsystem,
 * nie zmienia dostępu do niczego, co już działa.
 * <p>
 * CSRF wyłączone: statyczny frontend (fetch() z js/*.js) nie wysyła tokenów CSRF, a
 * włączenie domyślnej ochrony zablokowałoby wszystkie POST/PATCH na /listings/** (403).
 * To świadoma luka do domknięcia przy przeglądzie OWASP (patrz TODO.md), nie przeoczenie.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * Logowanie Google dodawane TYLKO gdy mamy prawdziwe dane — bez tego, próba
     * zarejestrowania `.oauth2Login()` z pustym/brakującym `ClientRegistrationRepository`
     * wywala start CAŁEJ aplikacji (sprawdzone empirycznie dwukrotnie: najpierw przez
     * `spring.security.oauth2.client.registration.*` property, potem przez pusty
     * `InMemoryClientRegistrationRepository` — oba rzucają przy starcie). Repozytorium
     * budowane bezpośrednio na konfiguratorze (`oauth2.clientRegistrationRepository(...)`),
     * bez osobnego beana — nie trzeba go wtedy w ogóle tworzyć, gdy brak danych.
     * <p>
     * WYMAGA prawdziwego Google Cloud OAuth client ID/secret (console.cloud.google.com)
     * ustawionego jako zmienne środowiskowe GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET —
     * bez nich logowanie Google jest po prostu wyłączone, reszta aplikacji (listings, GUI,
     * rejestracja/logowanie lokalne) działa normalnie.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                     SecurityContextRepository securityContextRepository,
                                                     GoogleLoginSuccessHandler googleLoginSuccessHandler,
                                                     @Value("${GOOGLE_CLIENT_ID:}") String googleClientId,
                                                     @Value("${GOOGLE_CLIENT_SECRET:}") String googleClientSecret) throws Exception {
        http
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())) // H2 console używa <frame>
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/*.html", "/css/**", "/js/**",
                                "/listings/**", "/auth/**", "/h2-console/**",
                                "/oauth2/**", "/login/**"
                        ).permitAll()
                        .anyRequest().authenticated());

        if (!googleClientId.isBlank() && !googleClientSecret.isBlank()) {
            ClientRegistration googleRegistration = CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientId(googleClientId)
                    .clientSecret(googleClientSecret)
                    .build();
            ClientRegistrationRepository repository = new InMemoryClientRegistrationRepository(List.of(googleRegistration));
            http.oauth2Login(oauth2 -> oauth2
                    .clientRegistrationRepository(repository)
                    .successHandler(googleLoginSuccessHandler));
            log.info("Logowanie przez Google skonfigurowane.");
        } else {
            log.warn("Brak GOOGLE_CLIENT_ID/GOOGLE_CLIENT_SECRET w środowisku — logowanie przez "
                    + "Google jest wyłączone. Oczekiwane bez prawdziwych danych z Google Cloud Console.");
        }

        return http.build();
    }
}
