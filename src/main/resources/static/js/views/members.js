/* Members — monthly pass registry. */
Views.members = function (root) {
    root.append(UI.el('div', { class: 'page-head' }, [
        UI.el('h1', {}, 'Members'),
        UI.el('p', {}, 'Monthly pass holders — member plates settle at zero at the express lane')
    ]));

    const table = UI.el('div', { class: 'panel' });
    root.append(table);

    async function render() {
        let members;
        try { members = await API.get('/api/members'); } catch (e) { UI.toast(e.error, 'err'); return; }
        UI.clear(table);
        table.append(UI.el('h3', {}, 'Pass holders (' + members.length + ')'));

        const tbl = UI.el('table', { class: 'tbl' });
        tbl.append(UI.el('tr', {}, ['ID', 'Name', 'Phone', 'Plates', 'Plan', 'Valid until', 'Status']
            .map(h => UI.el('th', {}, h))));
        members.forEach(m => {
            tbl.append(UI.el('tr', {}, [
                UI.el('td', { class: 'mono' }, m.id),
                UI.el('td', {}, m.name),
                UI.el('td', { class: 'mono' }, m.phone),
                UI.el('td', { class: 'mono small' }, m.plates.join(', ')),
                UI.el('td', {}, m.plan),
                UI.el('td', { class: 'mono' }, m.validUntil),
                UI.el('td', {}, m.expired
                    ? UI.el('span', { class: 'badge in' }, 'EXPIRED')
                    : UI.el('span', { class: 'badge out' }, 'valid'))
            ]));
        });
        table.append(tbl);

        if (API.isAdmin()) {
            const name = UI.el('input', { type: 'text', placeholder: 'Full name' });
            const phone = UI.el('input', { type: 'text', placeholder: 'Phone' });
            const plates = UI.el('input', { type: 'text', placeholder: 'Plate(s), comma separated' });
            const valid = UI.el('input', { type: 'date', value: new Date(Date.now() + 30 * 864e5).toISOString().slice(0, 10) });
            table.append(UI.el('div', { class: 'gate-form mt' }, [
                name, phone, plates, valid,
                UI.el('button', {
                    class: 'btn',
                    onclick: async () => {
                        try {
                            await API.post('/api/members', {
                                name: name.value, phone: phone.value,
                                plates: plates.value, validUntil: valid.value, plan: 'MONTHLY'
                            });
                            UI.toast('Member added', 'ok');
                            render();
                        } catch (e) { UI.toast(e.error, 'err'); }
                    }
                }, '＋ Add member')
            ]));
        } else {
            table.append(UI.el('div', { class: 'muted small mt' }, 'Sign in as ADMIN to edit the registry.'));
        }
    }

    render();
};
