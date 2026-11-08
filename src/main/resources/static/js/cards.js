// Współdzielone renderowanie karty ogłoszenia — używane przez index.js (wszystkie
// ogłoszenia) i changes.js (tylko te ze zmianami), żeby nie duplikować logiki.
const Cards = {
    statusBadgeClass(status) {
        switch (status) {
            case 'ACTIVE': return 'bg-success';
            case 'UNAVAILABLE': return 'bg-secondary';
            default: return 'bg-warning text-dark';
        }
    },

    renderCard(template, summary) {
        const node = template.content.cloneNode(true);

        const link = node.querySelector('.listing-link');
        // Jeśli są zmiany, kliknięcie karty skacze od razu do historii zmian na
        // stronie szczegółów, zamiast na górę strony — to jest to, co user chce zobaczyć.
        link.href = summary.changeCount > 0
            ? `listing.html?id=${summary.id}#changes-section`
            : `listing.html?id=${summary.id}`;

        const thumbSlot = node.querySelector('.thumb-slot');
        if (summary.mainPhotoUrl) {
            const img = document.createElement('img');
            img.src = summary.mainPhotoUrl;
            img.alt = Format.title(summary);
            img.className = 'listing-thumb';
            img.onerror = () => {
                img.remove();
                thumbSlot.innerHTML = '<div class="listing-thumb-placeholder">brak zdjęcia</div>';
            };
            thumbSlot.appendChild(img);
        } else {
            thumbSlot.innerHTML = '<div class="listing-thumb-placeholder">brak zdjęcia</div>';
        }

        node.querySelector('.title-slot').textContent = Format.title(summary);
        node.querySelector('.price-slot').textContent = Format.price(summary.price, summary.currency);
        node.querySelector('.mileage-slot').textContent = Format.mileage(summary.mileageKm);

        const statusEl = node.querySelector('.status-slot');
        statusEl.textContent = summary.status;
        statusEl.className = `badge status-slot ${Cards.statusBadgeClass(summary.status)}`;

        node.querySelector('.checked-slot').textContent = `sprawdzone: ${Format.dateTime(summary.lastCheckedAt)}`;

        // Duży, pełnoszerokościowy "przycisk" (div ze stylem btn — karta już jest
        // jednym dużym <a>, więc nie może zawierać zagnieżdżonego <a>/<button>).
        const changeCta = node.querySelector('.change-cta');
        if (summary.changeCount > 0) {
            changeCta.textContent = summary.changeCount === 1
                ? '⚠ 1 zmiana — zobacz →'
                : `⚠ ${summary.changeCount} zmiany — zobacz →`;
            changeCta.classList.remove('d-none');
        }

        return node;
    }
};
