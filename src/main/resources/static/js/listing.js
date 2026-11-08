(async function () {
    const params = new URLSearchParams(window.location.search);
    const id = params.get('id');

    const loading = document.getElementById('loading');
    const errorState = document.getElementById('error-state');
    const content = document.getElementById('content');

    if (!id) {
        loading.classList.add('d-none');
        errorState.textContent = 'Brak parametru ?id= w adresie strony.';
        errorState.classList.remove('d-none');
        return;
    }

    function statusBadgeClass(status) {
        switch (status) {
            case 'ACTIVE': return 'bg-success';
            case 'UNAVAILABLE': return 'bg-secondary';
            default: return 'bg-warning text-dark';
        }
    }

    // Jedno duże zdjęcie główne + dwa średnie (kolejne w ogłoszeniu) widoczne od razu;
    // reszta (może być ich kilkadziesiąt) schowana za "Pokaż więcej zdjęć", żeby nie
    // zajmować ekranów samymi miniaturkami o niskiej wartości.
    function renderPhotos(photos) {
        const featured = document.getElementById('photos-featured');
        const moreToggle = document.getElementById('photos-more-toggle');
        const moreGrid = document.getElementById('photos-more-grid');
        featured.innerHTML = '';
        moreGrid.innerHTML = '';

        if (!photos || photos.length === 0) {
            featured.innerHTML = '<div class="listing-thumb-placeholder" style="height:200px;">brak zdjęć</div>';
            moreToggle.classList.add('d-none');
            return;
        }

        const main = document.createElement('img');
        main.src = photos[0];
        main.className = 'photo-featured-main mb-2';
        main.loading = 'lazy';
        main.onerror = () => { main.remove(); };
        featured.appendChild(main);

        const mediumPhotos = photos.slice(1, 3);
        if (mediumPhotos.length > 0) {
            const row = document.createElement('div');
            row.className = 'row g-2';
            mediumPhotos.forEach(url => {
                const col = document.createElement('div');
                col.className = 'col-6';
                const img = document.createElement('img');
                img.src = url;
                img.className = 'photo-featured-medium';
                img.loading = 'lazy';
                img.onerror = () => { col.remove(); };
                col.appendChild(img);
                row.appendChild(col);
            });
            featured.appendChild(row);
        }

        const rest = photos.slice(3);
        if (rest.length > 0) {
            moreToggle.textContent = `Pokaż więcej zdjęć (${rest.length})`;
            moreToggle.classList.remove('d-none');
            rest.forEach(url => {
                const img = document.createElement('img');
                img.src = url;
                img.className = 'photo-thumb-small';
                img.loading = 'lazy';
                img.onerror = () => { img.remove(); };
                moreGrid.appendChild(img);
            });
        } else {
            moreToggle.classList.add('d-none');
        }
    }

    // Zwięzły callout na górze strony, zawsze widoczny (nawet przy braku zmian) —
    // pełna tabela "Historia zmian" jest niżej, ale user nie powinien musieć
    // przewijać całej strony (w tym dużego opisu), żeby się dowiedzieć, czy coś się zmieniło.
    function renderChangesSummary(changes) {
        const el = document.getElementById('changes-summary');
        const count = changes ? changes.length : 0;
        el.innerHTML = '';
        if (count === 0) {
            el.className = 'alert alert-secondary mb-3';
            el.textContent = 'Brak zmian od ostatniego sprawdzenia.';
            return;
        }
        el.className = 'alert alert-warning mb-3';
        el.append(count === 1 ? '1 zmiana wykryta od ostatniego sprawdzenia — ' : `${count} zmian wykrytych od ostatniego sprawdzenia — `);
        const link = document.createElement('a');
        link.href = '#changes-section';
        link.textContent = 'zobacz szczegóły poniżej';
        el.appendChild(link);
        el.append('.');
    }

    function appendCell(row, text) {
        const td = document.createElement('td');
        td.textContent = text;
        row.appendChild(td);
    }

    function renderSnapshots(snapshots) {
        const body = document.getElementById('snapshots-body');
        body.innerHTML = '';
        snapshots.forEach(s => {
            const tr = document.createElement('tr');
            appendCell(tr, Format.dateTime(s.capturedAt));
            appendCell(tr, Format.price(s.price, s.currency));
            appendCell(tr, Format.mileage(s.mileageKm));
            body.appendChild(tr);
        });

        renderChart(snapshots);
    }

    function renderChart(snapshots) {
        const canvas = document.getElementById('price-chart');
        if (typeof Chart === 'undefined' || !canvas) return;

        const chronological = [...snapshots].reverse();
        const labels = chronological.map(s => Format.dateTime(s.capturedAt));
        const prices = chronological.map(s => s.price !== null ? Number(s.price) : null);

        if (chronological.length < 2) {
            canvas.closest('div').insertAdjacentHTML('beforeend',
                '<p class="text-muted small">Wykres pojawi się po co najmniej dwóch odczytach.</p>');
            return;
        }

        new Chart(canvas, {
            type: 'line',
            data: {
                labels,
                datasets: [{
                    label: 'Cena',
                    data: prices,
                    borderColor: '#0d6efd',
                    backgroundColor: 'rgba(13,110,253,0.1)',
                    tension: 0.1,
                    fill: true
                }]
            },
            options: {
                responsive: true,
                plugins: { legend: { display: false } },
                scales: { y: { beginAtZero: false } }
            }
        });
    }

    function renderChanges(changes) {
        const body = document.getElementById('changes-body');
        const noChanges = document.getElementById('no-changes');
        body.innerHTML = '';
        if (!changes || changes.length === 0) {
            noChanges.classList.remove('d-none');
            return;
        }
        changes.forEach(c => {
            // fieldName jest nasz (stała nazwa pola), ale oldValue/newValue bywają
            // surową treścią z ogłoszenia (np. opis sprzedawcy) — niezaufane, stąd
            // textContent (nie innerHTML) na każdej komórce, bez wyjątków.
            const tr = document.createElement('tr');
            appendCell(tr, Format.orDash(c.fieldName));
            const oldTd = document.createElement('td');
            oldTd.className = 'text-muted';
            oldTd.textContent = Format.changePreview(c.oldValue);
            oldTd.title = c.oldValue || '';
            tr.appendChild(oldTd);
            const newTd = document.createElement('td');
            newTd.className = 'fw-semibold';
            newTd.textContent = Format.changePreview(c.newValue);
            newTd.title = c.newValue || '';
            tr.appendChild(newTd);
            appendCell(tr, Format.dateTime(c.detectedAt));
            body.appendChild(tr);
        });
    }

    function renderListing(listing, latestSnapshot) {
        document.getElementById('title').textContent = latestSnapshot ? Format.title(latestSnapshot) : `Ogłoszenie #${listing.id}`;
        document.getElementById('source').textContent = listing.source;

        const statusEl = document.getElementById('status');
        statusEl.textContent = listing.status;
        statusEl.className = `badge ${statusBadgeClass(listing.status)}`;

        document.getElementById('last-checked').textContent = Format.dateTime(listing.lastCheckedAt);

        const originalUrl = document.getElementById('original-url');
        originalUrl.href = listing.url;
        originalUrl.textContent = `Zobacz oryginalne ogłoszenie (${listing.url}) →`;

        if (latestSnapshot) {
            document.getElementById('price').textContent = Format.price(latestSnapshot.price, latestSnapshot.currency);
            document.getElementById('mileage').textContent = Format.mileage(latestSnapshot.mileageKm);
            document.getElementById('fuel').textContent = Format.orDash(latestSnapshot.fuelType);
            document.getElementById('gearbox').textContent = Format.orDash(latestSnapshot.gearbox);
            document.getElementById('year').textContent = Format.orDash(latestSnapshot.productionYear);
            document.getElementById('location').textContent = Format.orDash(latestSnapshot.location);
            // description to surowy HTML z ogłoszenia (treść sprzedawcy, niezaufana) —
            // sanityzujemy przed wstawieniem do DOM-u, żeby uniknąć stored XSS.
            const rawDescription = latestSnapshot.description || '<em>brak opisu</em>';
            document.getElementById('description').innerHTML = DOMPurify.sanitize(rawDescription);
            renderPhotos(latestSnapshot.photos);
        }

        ExternalSearch.render(latestSnapshot);

        document.getElementById('vin-input').value = listing.vin || '';
        document.getElementById('date-input').value = listing.firstRegistrationDate || '';
    }

    function wireVinForm(listingId) {
        const form = document.getElementById('vin-form');
        const statusSpan = document.getElementById('vin-save-status');
        form.addEventListener('submit', async (ev) => {
            ev.preventDefault();
            statusSpan.textContent = 'Zapisywanie...';
            statusSpan.className = 'small ms-2 text-muted';
            try {
                const vin = document.getElementById('vin-input').value.trim();
                const firstRegistrationDate = document.getElementById('date-input').value.trim();
                await Api.updateDetails(listingId, { vin, firstRegistrationDate });
                statusSpan.textContent = 'Zapisano ✓';
                statusSpan.className = 'small ms-2 text-success';
            } catch (err) {
                statusSpan.textContent = err.message;
                statusSpan.className = 'small ms-2 text-danger';
            }
        });
    }

    try {
        const [listing, snapshots, changes] = await Promise.all([
            Api.getListing(id),
            Api.getSnapshots(id),
            Api.getChanges(id)
        ]);

        loading.classList.add('d-none');
        content.classList.remove('d-none');

        renderListing(listing, snapshots.length > 0 ? snapshots[0] : null);
        renderChangesSummary(changes);
        renderSnapshots(snapshots);
        renderChanges(changes);
        wireVinForm(id);
    } catch (err) {
        loading.classList.add('d-none');
        errorState.textContent = `Nie udało się wczytać ogłoszenia: ${err.message}`;
        errorState.classList.remove('d-none');
    }
})();
