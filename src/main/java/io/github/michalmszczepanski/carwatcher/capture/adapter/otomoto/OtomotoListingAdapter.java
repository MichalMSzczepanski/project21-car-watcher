package io.github.michalmszczepanski.carwatcher.capture.adapter.otomoto;

import io.github.michalmszczepanski.carwatcher.capture.ListingCaptureException;
import io.github.michalmszczepanski.carwatcher.capture.ListingCapturePort;
import io.github.michalmszczepanski.carwatcher.capture.ParsedListingData;
import io.github.michalmszczepanski.carwatcher.domain.ListingSource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter dla otomoto.pl — implementacja {@link ListingCapturePort}.
 * <p>
 * Czyta wbudowany w stronę JSON z {@code <script id="__NEXT_DATA__">} (standard Next.js) i
 * nawiguje do {@code props.pageProps.advert}. Struktura zweryfikowana ręcznie na realnym
 * ogłoszeniu (patrz Context w planie) — to NIE są zgadywane selektory.
 */
@Component
public class OtomotoListingAdapter implements ListingCapturePort {

    private static final Logger log = LoggerFactory.getLogger(OtomotoListingAdapter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public boolean supports(ListingSource source) {
        return source == ListingSource.OTOMOTO;
    }

    @Override
    public ParsedListingData parse(String html, String sourceUrl) throws ListingCaptureException {
        log.debug("parse() start url={} htmlLength={}", sourceUrl, html == null ? 0 : html.length());
        JsonNode advert = extractAdvert(html, sourceUrl);
        Map<String, String> details = detailsMap(advert);
        log.debug("parse() znalezione klucze details: {}", details.keySet());

        ParsedListingData result = new ParsedListingData(
                textOrNull(advert.get("id")),
                textOrNull(advert.get("title")),
                textOrNull(advert.get("description")),
                priceValue(advert),
                priceCurrency(advert),
                extractIntOrNull(details.get("mileage")),
                details.get("make"),
                details.get("model"),
                details.get("version"),
                extractIntOrNull(details.get("year")),
                details.get("fuel_type"),
                details.get("gearbox"),
                location(advert),
                photos(advert)
        );
        log.debug("parse() wynik: make={} model={} price={} location={} zdjęć={}",
                result.make(), result.model(), result.price(), result.location(), result.photos().size());
        return result;
    }

    private JsonNode extractAdvert(String html, String sourceUrl) {
        Document document = Jsoup.parse(html);
        Element script = document.selectFirst("script#__NEXT_DATA__");
        if (script == null) {
            log.warn("extractAdvert() brak script#__NEXT_DATA__ dla url={} (długość html={})",
                    sourceUrl, html == null ? 0 : html.length());
            throw new ListingCaptureException(
                    "Brak __NEXT_DATA__ na stronie " + sourceUrl + " — struktura otomoto mogła się zmienić");
        }

        JsonNode root;
        try {
            root = MAPPER.readTree(script.data());
        } catch (Exception e) {
            log.warn("extractAdvert() niepoprawny JSON w __NEXT_DATA__ dla url={}: {}", sourceUrl, e.getMessage());
            throw new ListingCaptureException("Niepoprawny JSON w __NEXT_DATA__ dla " + sourceUrl, e);
        }

        JsonNode advert = root.path("props").path("pageProps").path("advert");
        if (advert.isMissingNode() || advert.isNull() || !advert.isObject()) {
            log.warn("extractAdvert() brak props.pageProps.advert dla url={}", sourceUrl);
            throw new ListingCaptureException(
                    "Brak danych ogłoszenia (props.pageProps.advert) dla " + sourceUrl);
        }
        return advert;
    }

    private Map<String, String> detailsMap(JsonNode advert) {
        Map<String, String> result = new HashMap<>();
        for (JsonNode detail : advert.path("details")) {
            String key = textOrNull(detail.get("key"));
            String value = textOrNull(detail.get("value"));
            if (key != null && value != null && !value.isBlank()) {
                result.put(key, value);
            }
        }
        return result;
    }

    private BigDecimal priceValue(JsonNode advert) {
        String raw = textOrNull(advert.path("price").get("value"));
        if (raw == null) {
            return null;
        }
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String priceCurrency(JsonNode advert) {
        return textOrNull(advert.path("price").get("currency"));
    }

    /**
     * Zweryfikowana ścieżka (druga realna strona, inna niż ta z pierwszego researchu):
     * {@code advert.seller.location.city} / {@code .region}.
     */
    private String location(JsonNode advert) {
        JsonNode locationNode = advert.path("seller").path("location");
        String city = textOrNull(locationNode.get("city"));
        String region = textOrNull(locationNode.get("region"));
        if (city != null && region != null) {
            return city + ", " + region;
        }
        return city != null ? city : region;
    }

    private List<String> photos(JsonNode advert) {
        List<String> result = new ArrayList<>();
        for (JsonNode photo : advert.path("images").path("photos")) {
            String url = textOrNull(photo.get("url"));
            if (url != null) {
                result.add(url);
            }
        }
        return result;
    }

    private static String textOrNull(JsonNode node) {
        return (node == null || node.isNull() || node.isMissingNode()) ? null : node.asText();
    }

    private static Integer extractIntOrNull(String raw) {
        if (raw == null) {
            return null;
        }
        String digitsOnly = raw.replaceAll("[^0-9]", "");
        if (digitsOnly.isEmpty()) {
            return null;
        }
        return Integer.parseInt(digitsOnly);
    }
}
