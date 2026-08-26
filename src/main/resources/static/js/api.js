/* ParkSense API client — bearer-token fetch wrapper. */
const API = {
    token: localStorage.getItem('ps_token') || null,
    user: JSON.parse(localStorage.getItem('ps_user') || 'null'),

    /**
     * Normalise any thrown value into {status, error} so views can always
     * print e.error — "undefined" never reaches a toast again.
     */
    normaliseError(e, fallback) {
        if (e && typeof e === 'object' && e.error) return e;
        const message = (e && (e.message || e.error)) || fallback || 'Unknown error';
        return { status: (e && e.status) || 0, error: message };
    },

    async request(method, path, body) {
        const headers = { 'Content-Type': 'application/json' };
        if (this.token) headers['Authorization'] = 'Bearer ' + this.token;
        let res;
        try {
            res = await fetch(path, {
                method,
                headers,
                body: body === undefined ? undefined : JSON.stringify(body)
            });
        } catch (networkError) {
            // server stopped / unreachable — a TypeError with no .error of its own
            if (typeof UI !== 'undefined') UI.setOffline(true);
            throw {
                status: 0,
                error: 'Server unreachable — is ParkSense running? (mvnw spring-boot:run → http://localhost:8080)'
            };
        }
        if (typeof UI !== 'undefined') UI.setOffline(false);
        if (res.status === 401 && location.hash !== '#/login') {
            this.logout(false);
            location.hash = '#/login';
            throw { status: 401, error: 'Session expired — sign in again' };
        }
        let data = null;
        try { data = await res.json(); } catch (e) { /* non-JSON (csv) */ }
        if (!res.ok) {
            throw { status: res.status, error: (data && data.error) || ('HTTP ' + res.status) };
        }
        return data;
    },

    get(path) { return this.request('GET', path); },
    post(path, body) { return this.request('POST', path, body); },
    put(path, body) { return this.request('PUT', path, body); },
    del(path) { return this.request('DELETE', path); },

    async login(username, password) {
        const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        const data = await res.json();
        if (!res.ok) throw { status: res.status, error: data.error || 'Login failed' };
        this.token = data.token;
        this.user = data;
        localStorage.setItem('ps_token', data.token);
        localStorage.setItem('ps_user', JSON.stringify(data));
        return data;
    },

    logout(redirect) {
        if (this.token) {
            fetch('/api/auth/logout', { method: 'POST', headers: { 'Authorization': 'Bearer ' + this.token } });
        }
        this.token = null;
        this.user = null;
        localStorage.removeItem('ps_token');
        localStorage.removeItem('ps_user');
        if (redirect !== false) location.hash = '#/login';
    },

    isAdmin() { return this.user && this.user.role === 'ADMIN'; },
    isDev() { return this.user && this.user.role === 'DEVELOPER'; }
};
