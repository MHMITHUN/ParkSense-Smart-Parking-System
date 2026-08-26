/* ParkSense live store — polls the cheap summary endpoints and notifies views. */
const Store = {
    summary: null,
    dashboard: null,
    events: [],
    boards: [],
    _timer: null,
    _listeners: [],

    onChange(cb) {
        this._listeners.push(cb);
        return () => { this._listeners = this._listeners.filter(l => l !== cb); };
    },

    emit() { this._listeners.forEach(cb => { try { cb(); } catch (e) { /* view swapped */ } }); },

    async refresh() {
        if (!API.token) return;
        try {
            const [summary, dashboard, events] = await Promise.all([
                API.get('/api/map/summary'),
                API.get('/api/reports/dashboard'),
                API.get('/api/system/events?limit=30')
            ]);
            this.summary = summary;
            this.dashboard = dashboard;
            this.events = events;
            this.emit();
        } catch (e) { /* transient — next tick retries */ }
    },

    startPolling() {
        this.stopPolling();
        this.refresh();
        this._timer = setInterval(() => this.refresh(), 3000);
    },

    stopPolling() {
        if (this._timer) { clearInterval(this._timer); this._timer = null; }
    }
};
