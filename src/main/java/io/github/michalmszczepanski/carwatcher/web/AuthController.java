package io.github.michalmszczepanski.carwatcher.web;

import io.github.michalmszczepanski.carwatcher.domain.User;
import io.github.michalmszczepanski.carwatcher.service.UserService;
import io.github.michalmszczepanski.carwatcher.web.dto.LoginRequest;
import io.github.michalmszczepanski.carwatcher.web.dto.MessageResponse;
import io.github.michalmszczepanski.carwatcher.web.dto.RegisterRequest;
import io.github.michalmszczepanski.carwatcher.web.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Logowanie sesyjne (cookie), nie JWT — prostsze i bezpieczniejsze dla appki serwowanej
 * z tego samego originu co statyczny frontend. Rejestracja/logowanie trzymane jako JSON,
 * nie formularze Springa, żeby pasowało do stylu reszty API (fetch() z frontend.js).
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(UserService userService, SecurityContextRepository securityContextRepository) {
        this.userService = userService;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping(path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> register(@RequestBody @Valid RegisterRequest request) {
        log.debug("POST /auth/register email={}", request.email());
        User user = userService.register(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @GetMapping("/verify")
    public ResponseEntity<MessageResponse> verify(@RequestParam String token) {
        log.debug("GET /auth/verify");
        userService.verifyEmail(token);
        return ResponseEntity.ok(new MessageResponse("E-mail zweryfikowany — możesz się teraz zalogować."));
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> login(@RequestBody @Valid LoginRequest request,
                                                HttpServletRequest httpRequest,
                                                HttpServletResponse httpResponse) {
        log.debug("POST /auth/login email={}", request.email());
        User user = userService.login(request.email(), request.password());
        establishSession(user, httpRequest, httpResponse);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * Ręczne ustawienie SecurityContext po udanym logowaniu przez nasz własny JSON endpoint
     * (nie przez domyślny formularz Spring Security) — standardowy wzorzec z dokumentacji
     * Spring Security dla customowego uwierzytelniania. Bez ról/uprawnień na tym etapie
     * (YAGNI — nie ma jeszcze nic do autoryzowania per rola).
     */
    private void establishSession(User user, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
