// To samo API co dashboard (GET /listings), tylko odfiltrowane do tych z
// wykrytymi zmianami — zero nowego endpointu backendowego, dane już tam są
// (ListingSummaryResponse.changeCount).
(async function () {
    const loading = document.getElementById('loading');
    const emptyState = document.getElementById('empty-state');
    const errorState = document.getElementById('error-state');
    const listingsEl = document.getElementById('listings');
    const template = document.getElementById('listing-card-template');

    try {
        const listings = await Api.getListings();
        loading.classList.add('d-none');

        const changed = (listings || []).filter(summary => summary.changeCount > 0);

        if (changed.length === 0) {
            emptyState.classList.remove('d-none');
            return;
        }

        // Najwięcej zmian na górze — to te ogłoszenia najbardziej "żyją".
        changed.sort((a, b) => b.changeCount - a.changeCount);
        changed.forEach(summary => listingsEl.appendChild(Cards.renderCard(template, summary)));
    } catch (err) {
        loading.classList.add('d-none');
        errorState.textContent = `Nie udało się wczytać listy zmian: ${err.message}`;
        errorState.classList.remove('d-none');
    }
})();
