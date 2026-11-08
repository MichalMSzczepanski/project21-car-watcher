package io.github.michalmszczepanski.carwatcher.capture;

import io.github.michalmszczepanski.carwatcher.domain.ListingSource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListingCapturePortResolver {

    private final List<ListingCapturePort> ports;

    public ListingCapturePortResolver(List<ListingCapturePort> ports) {
        this.ports = ports;
    }

    public ListingCapturePort resolve(ListingSource source) {
        return ports.stream()
                .filter(port -> port.supports(source))
                .findFirst()
                .orElseThrow(() -> new ListingCaptureException(
                        "Brak adaptera obsługującego źródło: " + source));
    }
}
