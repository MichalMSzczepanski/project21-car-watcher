package io.github.michalmszczepanski.carwatcher.web;

import io.github.michalmszczepanski.carwatcher.web.dto.CaptureRequest;
import io.github.michalmszczepanski.carwatcher.web.dto.UpdateListingDetailsRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ListingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void capturePierwszyRazTworzyNowyListingBezZmian() throws Exception {
        String html = readFixture("synthetic-next-data.html");
        String body = objectMapper.writeValueAsString(
                new CaptureRequest("https://www.otomoto.pl/oferta/test-controller-1.html", html));

        mockMvc.perform(post("/listings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newListing").value(true))
                .andExpect(jsonPath("$.changes").isEmpty());
    }

    @Test
    void capturePonownieTymSamymUrlWykrywaZmianeCeny() throws Exception {
        String url = "https://www.otomoto.pl/oferta/test-controller-2.html";
        String html1 = readFixture("synthetic-next-data.html");
        String html2 = readFixture("synthetic-next-data-price-changed.html");

        mockMvc.perform(post("/listings").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CaptureRequest(url, html1))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/listings").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CaptureRequest(url, html2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newListing").value(false))
                .andExpect(jsonPath("$.changes.length()").value(1))
                .andExpect(jsonPath("$.changes[0].fieldName").value("price"));
    }

    @Test
    void dlugiOpisWZmianieNieWywalaSie500() throws Exception {
        // Regresja: ListingChange.oldValue/newValue bez @Lob domyślnie to VARCHAR(255) —
        // realne opisy ogłoszeń regularnie są dłuższe i zapis się wywalał na 500.
        String url = "https://www.otomoto.pl/oferta/test-controller-dlugi-opis.html";
        String krotkiOpis = "Krótki opis.";
        String dlugiOpis = "x".repeat(2000);

        mockMvc.perform(post("/listings").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CaptureRequest(url, nextDataHtml(krotkiOpis)))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/listings").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CaptureRequest(url, nextDataHtml(dlugiOpis)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changes[?(@.fieldName=='description')].newValue").value(dlugiOpis));
    }

    private String nextDataHtml(String description) {
        return """
                <html><body><script id="__NEXT_DATA__" type="application/json">
                {"props":{"pageProps":{"advert":{
                  "id":"1",
                  "title":"Test",
                  "description":"%s",
                  "price":{"value":"10000","currency":"PLN"},
                  "images":{"photos":[]},
                  "details":[{"key":"make","value":"Opel"}]
                }}}}
                </script></body></html>
                """.formatted(description);
    }

    @Test
    void pobieranieHistoriiSnapshotowIZmian() throws Exception {
        String url = "https://www.otomoto.pl/oferta/test-controller-3.html";
        String html1 = readFixture("synthetic-next-data.html");
        String html2 = readFixture("synthetic-next-data-price-changed.html");

        String firstResponse = mockMvc.perform(post("/listings").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CaptureRequest(url, html1))))
                .andReturn().getResponse().getContentAsString();
        long listingId = objectMapper.readTree(firstResponse).get("listingId").asLong();

        mockMvc.perform(post("/listings").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CaptureRequest(url, html2))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/listings/{id}", listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(listingId));

        mockMvc.perform(get("/listings/{id}/snapshots", listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/listings/{id}/changes", listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void pobranieNieistniejacegoListinguDaje404() throws Exception {
        mockMvc.perform(get("/listings/{id}", 999_999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void pustyHtmlDaje400() throws Exception {
        String body = objectMapper.writeValueAsString(
                new CaptureRequest("https://www.otomoto.pl/oferta/test-controller-4.html", ""));

        mockMvc.perform(post("/listings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void niepoprawnyJsonDaje400ANie500() throws Exception {
        // np. brakująca wartość pola — realny przypadek: pusta zmienna powłoki w curlu
        String malformedJson = "{\"url\":\"https://www.otomoto.pl/oferta/test-controller-5.html\",\"html\":}";

        mockMvc.perform(post("/listings").contentType(MediaType.APPLICATION_JSON).content(malformedJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recznaAktualizacjaVinIDatyRejestracji() throws Exception {
        String url = "https://www.otomoto.pl/oferta/test-controller-6.html";
        String html = readFixture("synthetic-next-data.html");

        String captureResponse = mockMvc.perform(post("/listings").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CaptureRequest(url, html))))
                .andReturn().getResponse().getContentAsString();
        long listingId = objectMapper.readTree(captureResponse).get("listingId").asLong();

        String patchBody = objectMapper.writeValueAsString(
                new UpdateListingDetailsRequest("WVWZZZ1KZAW123456", LocalDate.of(2015, 6, 1)));

        mockMvc.perform(patch("/listings/{id}", listingId).contentType(MediaType.APPLICATION_JSON).content(patchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vin").value("WVWZZZ1KZAW123456"))
                .andExpect(jsonPath("$.firstRegistrationDate").value("2015-06-01"));

        mockMvc.perform(get("/listings/{id}", listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vin").value("WVWZZZ1KZAW123456"));
    }

    @Test
    void niepoprawnyVinDaje400() throws Exception {
        String url = "https://www.otomoto.pl/oferta/test-controller-7.html";
        String html = readFixture("synthetic-next-data.html");

        String captureResponse = mockMvc.perform(post("/listings").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CaptureRequest(url, html))))
                .andReturn().getResponse().getContentAsString();
        long listingId = objectMapper.readTree(captureResponse).get("listingId").asLong();

        String patchBody = objectMapper.writeValueAsString(new UpdateListingDetailsRequest("za-krotki-vin", null));

        mockMvc.perform(patch("/listings/{id}", listingId).contentType(MediaType.APPLICATION_JSON).content(patchBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("vin: VIN musi mieć 17 znaków, bez liter I, O, Q"))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("MethodArgumentNotValidException"))));
    }

    @Test
    void listaWszystkichOgloszenZawieraNowoDodany() throws Exception {
        String url = "https://www.otomoto.pl/oferta/test-controller-8.html";
        String html = readFixture("synthetic-next-data.html");

        mockMvc.perform(post("/listings").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CaptureRequest(url, html))))
                .andExpect(status().isOk());

        String filter = "$[?(@.url == '" + url + "')]";
        mockMvc.perform(get("/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filter).exists())
                .andExpect(jsonPath(filter + ".make").value(org.hamcrest.Matchers.hasItem("Ford")))
                .andExpect(jsonPath(filter + ".model").value(org.hamcrest.Matchers.hasItem("Focus")))
                .andExpect(jsonPath(filter + ".mainPhotoUrl").value(
                        org.hamcrest.Matchers.hasItem("https://example.test/photo1.jpg")));
    }

    @Test
    void listaWszystkichOgloszenPokazujeLiczbeZmian() throws Exception {
        String url = "https://www.otomoto.pl/oferta/test-controller-9.html";
        String html1 = readFixture("synthetic-next-data.html");
        String html2 = readFixture("synthetic-next-data-price-changed.html");

        mockMvc.perform(post("/listings").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CaptureRequest(url, html1))))
                .andExpect(status().isOk());

        String filter = "$[?(@.url == '" + url + "')]";
        mockMvc.perform(get("/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filter + ".changeCount").value(org.hamcrest.Matchers.hasItem(0)));

        mockMvc.perform(post("/listings").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CaptureRequest(url, html2))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filter + ".changeCount").value(org.hamcrest.Matchers.hasItem(1)));
    }

    private String readFixture(String name) {
        String path = "/fixtures/otomoto/" + name;
        try (InputStream in = getClass().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Brak fixture na classpath: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
