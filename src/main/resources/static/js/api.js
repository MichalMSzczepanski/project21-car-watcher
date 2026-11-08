// Cienka warstwa nad fetch() do naszego REST API. Zero zależności, zero build toola.
const Api = {
    async getListings() {
        return Api._json(await fetch('/listings'));
    },

    async getListing(id) {
        return Api._json(await fetch(`/listings/${id}`));
    },

    async getSnapshots(id) {
        return Api._json(await fetch(`/listings/${id}/snapshots`));
    },

    async getChanges(id) {
        return Api._json(await fetch(`/listings/${id}/changes`));
    },

    async updateDetails(id, { vin, firstRegistrationDate }) {
        const response = await fetch(`/listings/${id}`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ vin: vin || null, firstRegistrationDate: firstRegistrationDate || null })
        });
        if (!response.ok) {
            const error = await Api._json(response).catch(() => null);
            throw new Error(error && error.message ? error.message : `Błąd ${response.status}`);
        }
        return Api._json(response);
    },

    async _json(response) {
        if (!response.ok) {
            throw new Error(`Błąd ${response.status}`);
        }
        return response.json();
    }
};
