/* Tariff admin — plan cards + the builder form. */
Views.tariffs = function (root) {
    root.append(UI.el('div', { class: 'page-head' }, [
        UI.el('h1', {}, 'Tariff Plans'),
        UI.el('p', {}, 'One active plan per scheme — the exit lane prices by whichever is switched on')
    ]));

    const cards = UI.el('div', { class: 'grid cols-3' });
    root.append(cards);
    if (API.isAdmin()) {
        root.append(UI.el('button', { class: 'btn mt', onclick: () => builderModal(null) }, '＋ New plan (builder)'));
    }

    async function render() {
        let plans;
        try { plans = await API.get('/api/tariffs'); } catch (e) { UI.toast(e.error, 'err'); return; }
        UI.clear(cards);
        plans.forEach(p => {
            const fees = [];
            fees.push('base ৳' + UI.money(p.baseFee));
            fees.push('৳' + UI.money(p.perHour) + '/h');
            if (p.dailyCap) fees.push('cap ৳' + UI.money(p.dailyCap));
            if (p.flatFee) fees.push('flat ৳' + UI.money(p.flatFee));
            if (p.surgeMultiplier !== '1') fees.push('×' + p.surgeMultiplier + ' surge');
            fees.push(p.graceMinutes + 'm grace');

            const card = UI.el('div', { class: 'panel' }, [
                UI.el('div', { class: 'row spread' }, [
                    UI.el('h3', { style: 'margin:0' }, p.name),
                    UI.el('span', { class: 'badge ' + (p.active ? 'out' : 'note') }, p.active ? 'ACTIVE' : 'off')
                ]),
                UI.el('div', { class: 'muted small', style: 'margin:8px 0' },
                    p.kindLabel + ' · ' + p.id),
                UI.el('div', { class: 'small mono', style: 'line-height:1.9' }, fees.join('  ·  '))
            ]);

            if (API.isAdmin()) {
                card.append(UI.el('div', { class: 'row mt' }, [
                    UI.el('button', {
                        class: 'btn small ' + (p.active ? 'secondary' : ''),
                        onclick: async () => {
                            try {
                                await API.post(`/api/tariffs/${p.id}/${p.active ? 'deactivate' : 'activate'}`);
                                UI.toast(p.active ? 'Deactivated' : 'Activated — now pricing exits', 'ok');
                                render();
                            } catch (e) { UI.toast(e.error, 'err'); }
                        }
                    }, p.active ? 'Deactivate' : 'Activate')
                ]));
            }
            cards.append(card);
        });
    }

    function builderModal() {
        const kind = UI.el('select', {}, [
            ['HOURLY', 'Hourly'], ['DAILY_CAP', 'Hourly + daily cap'], ['EARLY_BIRD', 'Early-bird flat'],
            ['EVENT_SURGE', 'Event surge'], ['MEMBER_PASS', 'Member pass']
        ].map(([v, l]) => UI.el('option', { value: v }, l)));
        const fields = {};
        ['baseFee', 'perHour', 'dailyCap', 'graceMinutes', 'flatFee', 'surgeMultiplier', 'earlyIn', 'earlyOut'].forEach(f => {
            fields[f] = UI.el('input', { type: 'text', placeholder: f });
        });
        fields.baseFee.value = '20'; fields.perHour.value = '30'; fields.graceMinutes.value = '15';
        const err = UI.el('div', { class: 'login-err' });

        const form = UI.el('div', { class: 'grid cols-2' }, [
            UI.el('label', { class: 'fld' }, ['Kind', kind]),
            UI.el('label', { class: 'fld' }, ['Base fee (৳)', fields.baseFee]),
            UI.el('label', { class: 'fld' }, ['Per hour (৳)', fields.perHour]),
            UI.el('label', { class: 'fld' }, ['Daily cap (৳)', fields.dailyCap]),
            UI.el('label', { class: 'fld' }, ['Grace minutes', fields.graceMinutes]),
            UI.el('label', { class: 'fld' }, ['Flat fee (৳, early-bird)', fields.flatFee]),
            UI.el('label', { class: 'fld' }, ['Surge ×', fields.surgeMultiplier]),
            UI.el('label', { class: 'fld' }, ['Early in before (HH:MM)', fields.earlyIn]),
            UI.el('label', { class: 'fld' }, ['Early out after (HH:MM)', fields.earlyOut]),
            err
        ]);

        UI.modal('Tariff plan builder', form, [
            { label: 'Cancel' },
            {
                label: 'Build & save', kind: '',
                onclick: async (close) => {
                    err.textContent = '';
                    const body = {
                        id: 'TAR-CUSTOM-' + Date.now() % 1000,
                        name: 'Custom ' + kind.value,
                        kind: kind.value,
                        baseFee: fields.baseFee.value || null,
                        perHour: fields.perHour.value || null,
                        dailyCap: fields.dailyCap.value || null,
                        graceMinutes: fields.graceMinutes.value || null,
                        flatFee: fields.flatFee.value || null,
                        surgeMultiplier: fields.surgeMultiplier.value || null,
                        earlyIn: fields.earlyIn.value || null,
                        earlyOut: fields.earlyOut.value || null,
                        active: false
                    };
                    Object.keys(body).forEach(k => { if (body[k] === null) delete body[k]; });
                    try {
                        await API.post('/api/tariffs', body);
                        UI.toast('Plan built and saved', 'ok');
                        close(); render();
                    } catch (e) { err.textContent = e.error; }
                }
            }
        ]);
    }

    render();
};
