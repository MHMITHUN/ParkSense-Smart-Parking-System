/* ParkSense hash router. (Views registry is declared in ui.js, loaded earlier.) */
const ROUTES = {
    '#/login': 'login',
    '#/dashboard': 'dashboard',
    '#/map': 'map',
    '#/gates': 'gates',
    '#/tickets': 'tickets',
    '#/members': 'members',
    '#/tariffs': 'tariffs',
    '#/reports': 'reports',
    '#/patterns': 'patterns'
};

function renderRoute() {
    const hash = location.hash || '#/login';
    let name = ROUTES[hash];
    if (!API.token) name = 'login';
    else if (!name || name === 'login') name = hash === '#/login' ? 'dashboard' : (name || 'dashboard');

    // Restrict Patterns view to DEVELOPER role only
    if (name === 'patterns' && !API.isDev()) {
        UI.toast('Patterns guide is restricted to Developer login', 'warn');
        location.hash = '#/dashboard';
        return;
    }

    const view = Views[name] || Views.login;
    const root = document.getElementById('view');
    UI.clear(root);

    const nav = document.getElementById('nav');
    const chip = document.getElementById('user-chip');
    const logoutBtn = document.getElementById('logout-btn');
    if (API.token) {
        nav.classList.remove('hidden');
        chip.classList.remove('hidden');
        logoutBtn.classList.remove('hidden');
        chip.innerHTML = '';
        chip.append(UI.el('span', {}, [
            document.createTextNode(API.user.fullName + ' · '),
            UI.el('b', {}, API.user.role)
        ]));

        // Show Patterns tab only for DEVELOPER role
        const patternsTab = nav.querySelector('a[data-route="patterns"]');
        if (patternsTab) {
            patternsTab.style.display = API.isDev() ? '' : 'none';
        }

        Store.startPolling();
    } else {
        nav.classList.add('hidden');
        chip.classList.add('hidden');
        logoutBtn.classList.add('hidden');
        Store.stopPolling();
    }
    nav.querySelectorAll('a').forEach(a => {
        a.classList.toggle('active', a.dataset.route === name);
    });

    try {
        view(root);
    } catch (e) {
        root.append(UI.el('div', { class: 'panel' }, 'View failed: ' + e.message));
    }
}

document.getElementById('logout-btn').addEventListener('click', () => API.logout());
window.addEventListener('hashchange', renderRoute);
renderRoute();
