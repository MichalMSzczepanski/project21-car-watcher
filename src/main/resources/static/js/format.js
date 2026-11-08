const Format = {
    price(value, currency) {
        if (value === null || value === undefined) return '—';
        const num = Number(value);
        return `${num.toLocaleString('pl-PL')} ${currency || ''}`.trim();
    },

    mileage(km) {
        if (km === null || km === undefined) return '—';
        return `${Number(km).toLocaleString('pl-PL')} km`;
    },

    dateTime(iso) {
        if (!iso) return '—';
        const d = new Date(iso);
        return d.toLocaleString('pl-PL');
    },

    date(isoDate) {
        if (!isoDate) return '—';
        return isoDate; // yyyy-MM-dd, czytelne wprost
    },

    title(summaryOrSnapshot) {
        const parts = [summaryOrSnapshot.make, summaryOrSnapshot.model, summaryOrSnapshot.version]
            .filter(p => p && p.trim().length > 0);
        if (parts.length > 0) return parts.join(' ');
        return summaryOrSnapshot.title || 'Ogłoszenie bez tytułu';
    },

    orDash(value) {
        return (value === null || value === undefined || value === '') ? '—' : value;
    },

    // Podgląd wartości w tabeli "Historia zmian" — pola typu "description" bywają
    // długim HTML-em sprzedawcy (setki/tysiące znaków). Renderujemy to przez
    // textContent (bezpiecznie, bez wykonywania znaczników), ale surowe <p>/<span>
    // w zwykłym tekście wyglądają nieczytelnie, więc dla podglądu zdejmujemy
    // znaczniki i skracamy — to tylko podgląd w tabeli, nie jedyne źródło danych.
    changePreview(value, maxLength = 160) {
        if (value === null || value === undefined || value === '') return '—';
        const stripped = String(value).replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim();
        if (stripped.length === 0) return '—';
        return stripped.length > maxLength ? stripped.slice(0, maxLength) + '…' : stripped;
    }
};
