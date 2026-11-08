package io.github.michalmszczepanski.carwatcher.capture.adapter.otomoto;

import io.github.michalmszczepanski.carwatcher.capture.ListingCaptureException;
import io.github.michalmszczepanski.carwatcher.capture.ParsedListingData;
import io.github.michalmszczepanski.carwatcher.domain.ListingSource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OtomotoListingAdapterTest {

    private final OtomotoListingAdapter adapter = new OtomotoListingAdapter();

    @Test
    void wspieraTylkoZrodloOtomoto() {
        assertThat(adapter.supports(ListingSource.OTOMOTO)).isTrue();
        assertThat(adapter.supports(ListingSource.OTHER)).isFalse();
    }

    @Test
    void parsujeWszystkieZmapowanePolaZeZweryfikowanejStruktury() {
        String html = readFixture("synthetic-next-data.html");

        ParsedListingData parsed = adapter.parse(html, "https://www.otomoto.pl/oferta/test.html");

        assertThat(parsed.externalId()).isEqualTo("999888777");
        assertThat(parsed.title()).isEqualTo("Testowy Ford Focus 1.6 TDCi");
        assertThat(parsed.description()).isEqualTo("<p>Opis testowy</p>");
        assertThat(parsed.price()).isEqualByComparingTo(new BigDecimal("45000"));
        assertThat(parsed.currency()).isEqualTo("PLN");
        assertThat(parsed.mileageKm()).isEqualTo(120000);
        assertThat(parsed.make()).isEqualTo("Ford");
        assertThat(parsed.model()).isEqualTo("Focus");
        assertThat(parsed.version()).isEqualTo("1.6 TDCi Trend");
        assertThat(parsed.productionYear()).isEqualTo(2015);
        assertThat(parsed.fuelType()).isEqualTo("Diesel");
        assertThat(parsed.gearbox()).isEqualTo("Manualna");
        assertThat(parsed.photos()).containsExactly(
                "https://example.test/photo1.jpg", "https://example.test/photo2.jpg");
    }

    @Test
    void brakNextDataRzucaWyjatek() {
        String html = "<html><head><title>Bez danych</title></head><body>brak scripta</body></html>";

        assertThatThrownBy(() -> adapter.parse(html, "https://www.otomoto.pl/oferta/test.html"))
                .isInstanceOf(ListingCaptureException.class);
    }

    @Test
    void brakujacePoleWDetailsDajeNullAResztaSieParsuje() {
        String html = """
                <html><body><script id="__NEXT_DATA__" type="application/json">
                {"props":{"pageProps":{"advert":{
                  "id":"1",
                  "title":"Tytuł",
                  "description":"Opis",
                  "price":{"value":"10000","currency":"PLN"},
                  "images":{"photos":[]},
                  "details":[
                    {"key":"make","value":"Opel"},
                    {"key":"model","value":"Astra"}
                  ]
                }}}}
                </script></body></html>
                """;

        ParsedListingData parsed = adapter.parse(html, "https://www.otomoto.pl/oferta/test.html");

        assertThat(parsed.make()).isEqualTo("Opel");
        assertThat(parsed.model()).isEqualTo("Astra");
        assertThat(parsed.mileageKm()).isNull();
        assertThat(parsed.fuelType()).isNull();
    }

    @Test
    void lokalizacjaSkladaSieZMiastaIRegionu() {
        String html = """
                <html><body><script id="__NEXT_DATA__" type="application/json">
                {"props":{"pageProps":{"advert":{
                  "id":"1",
                  "title":"Tytuł",
                  "description":"Opis",
                  "price":{"value":"10000","currency":"PLN"},
                  "images":{"photos":[]},
                  "details":[],
                  "seller":{"location":{"city":"Wola Moszczenicka","region":"Łódzkie"}}
                }}}}
                </script></body></html>
                """;

        ParsedListingData parsed = adapter.parse(html, "https://www.otomoto.pl/oferta/test.html");

        assertThat(parsed.location()).isEqualTo("Wola Moszczenicka, Łódzkie");
    }

    @Test
    void pustyAdvertRzucaWyjatek() {
        String html = """
                <html><body><script id="__NEXT_DATA__" type="application/json">
                {"props":{"pageProps":{}}}
                </script></body></html>
                """;

        assertThatThrownBy(() -> adapter.parse(html, "https://www.otomoto.pl/oferta/test.html"))
                .isInstanceOf(ListingCaptureException.class);
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
