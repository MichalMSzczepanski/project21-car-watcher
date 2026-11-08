// Generuje linki do wyszukiwania TEGO SAMEGO auta (marka/model/rocznik) na innych portalach.
// Czysto po stronie klienta — zero nowego endpointu backendowego, zero automatycznego
// pobierania (to tylko linki otwierane przez usera w nowej karcie, nie scraping).
//
// VIN pominięty celowo: sprzedawcy praktycznie nigdy nie publikują VIN-u w treści
// ogłoszenia, więc wyszukiwanie po samym VIN-ie dawałoby w większości przypadków 0 wyników.
//
// Wzorce URL zweryfikowane ręcznie (nie zgadywane):
// - OLX: ścieżkowy filtr marka/model (`/motoryzacja/samochody/{make}/{model}/`) — OLX Group,
//   ten sam standard co otomoto, potwierdzone realnym zapytaniem (463 wyniki dla kia/sorento).
// - Allegro Lokalnie i Sprzedajemy.pl: wyszukiwanie tekstowe, potwierdzone realnym zapytaniem
//   "Kia Sorento" (odpowiednio ~1562 i 95 wyników).
// - Autoplac.pl: potwierdzony tylko filtr marki (`/oferty/samochody-osobowe/{marka}`) —
//   wzorzec dla modelu NIE został zweryfikowany, więc go nie dodajemy (nie zgadujemy URL-i).
// - Gratka.pl: pominięte — strona odrzuciła pobranie podczas researchu, nie dało się
//   zweryfikować żadnego wzorca.
const ExternalSearch = {
    slug(value) {
        return value
            .toLowerCase()
            .trim()
            .replace(/[^a-z0-9\s-]/g, '')
            .replace(/\s+/g, '-');
    },

    buildLinks(snapshot) {
        if (!snapshot || !snapshot.make) {
            return [];
        }

        const make = snapshot.make.trim();
        const model = snapshot.model ? snapshot.model.trim() : null;
        const version = snapshot.version ? snapshot.version.trim() : null;

        const phrase = [make, model, version].filter(Boolean).join(' ');
        const makeSlug = encodeURIComponent(ExternalSearch.slug(make));

        const links = [];

        if (model) {
            const modelSlug = encodeURIComponent(ExternalSearch.slug(model));
            links.push({ name: 'OLX', url: `https://www.olx.pl/motoryzacja/samochody/${makeSlug}/${modelSlug}/` });
        } else {
            links.push({ name: 'OLX (tylko marka)', url: `https://www.olx.pl/motoryzacja/samochody/${makeSlug}/` });
        }

        links.push({
            name: 'Allegro Lokalnie',
            url: `https://allegrolokalnie.pl/oferty/motoryzacja/q/${encodeURIComponent(phrase)}`
        });

        links.push({
            name: 'Sprzedajemy.pl',
            url: `https://sprzedajemy.pl/motoryzacja/samochody-osobowe?inp_text[v]=${encodeURIComponent(phrase)}&inp_text[n]=1`
        });

        links.push({ name: 'Autoplac.pl (tylko marka)', url: `https://www.autoplac.pl/oferty/samochody-osobowe/${makeSlug}` });

        return links;
    },

    render(snapshot) {
        const container = document.getElementById('external-search-links');
        if (!container) {
            return;
        }

        const links = ExternalSearch.buildLinks(snapshot);
        if (links.length === 0) {
            container.textContent = 'Brak marki w danych ogłoszenia — nie da się zbudować wyszukiwania.';
            return;
        }

        links.forEach(link => {
            const a = document.createElement('a');
            a.href = link.url;
            a.target = '_blank';
            a.rel = 'noopener';
            a.className = 'btn btn-sm btn-outline-secondary text-start';
            a.textContent = `Szukaj na ${link.name} →`;
            container.appendChild(a);
        });
    }
};
