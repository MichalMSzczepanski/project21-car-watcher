package io.github.michalmszczepanski.carwatcher.web;

import io.github.michalmszczepanski.carwatcher.domain.User;
import io.github.michalmszczepanski.carwatcher.repository.UserRepository;
import io.github.michalmszczepanski.carwatcher.web.dto.LoginRequest;
import io.github.michalmszczepanski.carwatcher.web.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void rejestracjaTworzyNiezweryfikowanegoUsera() throws Exception {
        String email = "rejestracja1@example.com";
        String body = objectMapper.writeValueAsString(new RegisterRequest(email, "supersecret123"));

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.authProvider").value("LOCAL"));

        User saved = userRepository.findByEmail(email).orElseThrow();
        assertThat(saved.getVerificationToken()).isNotBlank();
        assertThat(saved.getPasswordHash()).isNotEqualTo("supersecret123");
    }

    @Test
    void duplikatEmailaDaje409() throws Exception {
        String email = "duplikat@example.com";
        String body = objectMapper.writeValueAsString(new RegisterRequest(email, "supersecret123"));

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void weryfikacjaPoprawnymTokenemOdblokowujeKonto() throws Exception {
        String email = "weryfikacja1@example.com";
        registerUser(email, "supersecret123");
        String token = userRepository.findByEmail(email).orElseThrow().getVerificationToken();

        mockMvc.perform(get("/auth/verify").param("token", token))
                .andExpect(status().isOk());

        User verified = userRepository.findByEmail(email).orElseThrow();
        assertThat(verified.isEmailVerified()).isTrue();
        assertThat(verified.getVerificationToken()).isNull();
    }

    @Test
    void zlyTokenWeryfikacyjnyDaje400() throws Exception {
        mockMvc.perform(get("/auth/verify").param("token", "nie-istnieje-taki-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginPrzedWeryfikacjaJestZablokowany() throws Exception {
        String email = "niezweryfikowany@example.com";
        registerUser(email, "supersecret123");

        String loginBody = objectMapper.writeValueAsString(new LoginRequest(email, "supersecret123"));
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginPoWeryfikacjiDzialaPoprawnie() throws Exception {
        String email = "zweryfikowany@example.com";
        String password = "supersecret123";
        registerUser(email, password);
        verifyUser(email);

        String loginBody = objectMapper.writeValueAsString(new LoginRequest(email, password));
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.emailVerified").value(true));
    }

    @Test
    void loginZeZlymHaslemDaje401() throws Exception {
        String email = "zleHaslo@example.com";
        registerUser(email, "supersecret123");
        verifyUser(email);

        String loginBody = objectMapper.writeValueAsString(new LoginRequest(email, "zupelnieInneHaslo"));
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isUnauthorized());
    }

    private void registerUser(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(new RegisterRequest(email, password));
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private void verifyUser(String email) throws Exception {
        String token = userRepository.findByEmail(email).orElseThrow().getVerificationToken();
        mockMvc.perform(get("/auth/verify").param("token", token))
                .andExpect(status().isOk());
    }
}
