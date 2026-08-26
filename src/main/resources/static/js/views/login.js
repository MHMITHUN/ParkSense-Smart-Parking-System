/* Login view — credentials in, bearer token out. */
Views.login = function (root) {
    const err = UI.el('div', { class: 'login-err' });

    const userIn = UI.el('input', { type: 'text', placeholder: 'username', autocomplete: 'username' });
    const passIn = UI.el('input', { type: 'password', placeholder: 'password', autocomplete: 'current-password' });

    async function submit() {
        err.textContent = '';
        try {
            await API.login(userIn.value.trim(), passIn.value);
            UI.toast('Signed in — welcome to the control room', 'ok');
            location.hash = '#/dashboard';
        } catch (e) {
            err.textContent = e.error || 'Login failed';
        }
    }

    function quickLogin(u, p) {
        userIn.value = u;
        passIn.value = p;
        submit();
    }

    const form = UI.el('div', {}, [
        UI.el('label', { class: 'fld' }, ['Username', userIn]),
        UI.el('label', { class: 'fld' }, ['Password', passIn]),
        UI.el('button', { class: 'btn login-btn', onclick: submit }, 'ENTER CONTROL ROOM'),
        err
    ]);
    form.addEventListener('keydown', e => { if (e.key === 'Enter') submit(); });

    const lgMark = UI.el('div', { class: 'lg-mark', title: 'ParkSense Enterprise' });
    lgMark.innerHTML = `<svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
        <rect x="2.5" y="2.5" width="19" height="19" rx="5.5" stroke="url(#loginBrandGrad)" stroke-width="2" fill="none" />
        <path d="M9 17V7h4.5a3.5 3.5 0 0 1 0 7H9" stroke="url(#loginBrandGrad)" stroke-width="2.4" />
        <circle cx="16.5" cy="16.5" r="1.5" fill="#38d9f5" />
        <defs>
            <linearGradient id="loginBrandGrad" x1="2.5" y1="2.5" x2="21.5" y2="21.5" gradientUnits="userSpaceOnUse">
                <stop stop-color="#38d9f5"/>
                <stop offset="1" stop-color="#818cf8"/>
            </linearGradient>
        </defs>
    </svg>`;

    root.append(UI.el('div', { class: 'login-wrap' }, [
        UI.el('div', { class: 'login-card' }, [
            lgMark,
            UI.el('h2', {}, ['PARK', UI.el('b', {}, 'SENSE')]),
            UI.el('div', { class: 'lg-sub' }, 'Smart Parking Management System'),
            form,
            UI.el('div', { class: 'demo-creds' }, [
                UI.el('div', { class: 'muted small', style: 'margin-bottom:8px; display:flex; justify-content:space-between; align-items:center;' }, [
                    'Demo accounts:',
                    UI.el('span', { style: 'color: var(--accent); font-size:11px; font-weight:600;' }, 'Quick select')
                ]),
                UI.el('table', { class: 'demo-table' }, [
                    UI.el('tr', {
                        class: 'demo-row',
                        title: 'Auto-fill & login as Admin',
                        onclick: () => quickLogin('admin', 'admin123')
                    }, [
                        UI.el('td', { class: 'mono' }, 'admin / admin123'),
                        UI.el('td', { style: 'text-align:right' }, 'Administrator →')
                    ]),
                    UI.el('tr', {
                        class: 'demo-row',
                        title: 'Auto-fill & login as Operator',
                        onclick: () => quickLogin('operator', 'operator123')
                    }, [
                        UI.el('td', { class: 'mono' }, 'operator / operator123'),
                        UI.el('td', { style: 'text-align:right' }, 'Gate Operator →')
                    ]),
                    UI.el('tr', {
                        class: 'demo-row',
                        title: 'Auto-fill & login as Developer (View Design Patterns)',
                        onclick: () => quickLogin('dev', 'dev123')
                    }, [
                        UI.el('td', { class: 'mono' }, 'dev / dev123'),
                        UI.el('td', { style: 'text-align:right' }, 'Developer (Patterns) →')
                    ])
                ])
            ])
        ])
    ]));
    userIn.focus();
};
