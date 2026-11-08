(async function () {
    const loading = document.getElementById('loading');
    const emptyState = document.getElementById('empty-state');
    const errorState = document.getElementById('error-state');
    const listingsEl = document.getElementById('listings');
    const template = document.getElementById('listing-card-template');

    try {
        const listings = await Api.getListings();
        loading.classList.add('d-none');

        if (!listings || listings.length === 0) {
            emptyState.classList.remove('d-none');
            return;
        }

        listings.forEach(summary => listingsEl.appendChild(Cards.renderCard(template, summary)));
    } catch (err) {
        loading.classList.add('d-none');
        errorState.textContent = `Nie udało się wczytać listy ogłoszeń: ${err.message}`;
        errorState.classList.remove('d-none');
    }
})();
