/* Control room dashboard — KPIs, entrance board, alerts, live feed. */
Views.dashboard = function (root) {
    root.append(UI.el('div', { class: 'page-head' }, [
        UI.el('h1', {}, 'Control Room'),
        UI.el('p', {}, 'Live overview of ParkSense Central Plaza — updates every few seconds')
    ]));

    const kpis = UI.el('div', { class: 'grid cols-5', id: 'dash-kpis' });
    const lower = UI.el('div', { class: 'grid cols-2 mt' }, [
        UI.el('div', { class: 'panel', id: 'dash-board' }, [UI.el('h3', {}, 'Entrance LED board')]),
        UI.el('div', { class: 'panel', id: 'dash-feed' }, [UI.el('h3', {}, 'Live activity feed')])
    ]);
    const alerts = UI.el('div', { class: 'panel mt', id: 'dash-alerts' }, [UI.el('h3', {}, 'Capacity alerts')]);
    root.append(kpis, lower, alerts);

    function render() {
        const d = Store.dashboard;
        const s = Store.summary;
        if (!d || !s) return;

        const k = document.getElementById('dash-kpis');
        if (!k) return; // view was swapped mid-render
        UI.clear(k);
        const tile = (cls, label, target, suffix, sub, money) => {
            const value = UI.el('div', { class: 'kpi-value' }, '0');
            const tileEl = UI.el('div', { class: 'kpi ' + cls }, [
                UI.el('div', { class: 'kpi-label' }, label),
                value,
                UI.el('div', { class: 'kpi-sub' }, sub)
            ]);
            if (money) {
                value.prepend(document.createTextNode('৳ '));
                UI.countUp(value, Number(target || 0), v => UI.money(v));
            } else {
                UI.countUp(value, Number(target || 0));
            }
            if (suffix) value.append(UI.el('small', {}, ' ' + suffix));
            return tileEl;
        };
        k.append(
            tile('good', 'Free bays', d.freeSlots, '/ ' + d.totalSlots,
                (d.totalSlots ? Math.round(d.freeSlots * 100 / d.totalSlots) : 0) + '% available'),
            tile('bad', 'Occupied now', d.occupiedSlots, null, 'vehicles inside'),
            tile('', 'Active tickets', d.activeTickets, null, 'open sessions'),
            tile('accent', 'Revenue today', d.revenueToday, null, 'settled fares', true),
            tile('', 'Members inside', d.membersInside, null, 'monthly passes')
        );

        API.get('/api/system/boards').then(boards => {
            const panel = document.getElementById('dash-board');
            UI.clear(panel);
            panel.append(UI.el('h3', {}, 'Entrance LED board'));
            const entrance = boards.find(b => b.boardId === 'BOARD-ENTRANCE') || boards[0];
            const full = entrance && entrance.lines.some(l => l.includes('FULL'));
            panel.append(UI.el('div', { class: 'led-board' + (full ? ' full' : '') }, [
                UI.el('div', { class: 'led-title' }, entrance ? entrance.boardId : '—'),
                ...(entrance ? entrance.lines.map(l => UI.el('div', {}, l)) : ['—'])
            ]));
            panel.append(UI.el('div', { class: 'muted small mt' }, 'Level boards:'));
            boards.filter(b => b.boardId !== 'BOARD-ENTRANCE').forEach(b => {
                panel.append(UI.el('div', { class: 'small mono', style: 'margin-top:4px;color:var(--free)' },
                    b.boardId + ' → ' + b.lines.join('  |  ')));
            });
        }).catch(() => {});

        const feedPanel = document.getElementById('dash-feed');
        UI.clear(feedPanel);
        feedPanel.append(UI.el('h3', {}, 'Live activity feed'));
        const feed = UI.el('div', { class: 'feed' });
        if (!Store.events.length) {
            feed.append(UI.el('div', { class: 'muted small' }, 'No occupancy events yet — simulate an entry at the Gates page.'));
        }
        Store.events.forEach(e => {
            const isEntry = e.transition.includes('OCCUPIED');
            const isExit = e.transition.includes('FREE');
            feed.append(UI.el('div', { class: 'feed-row' }, [
                UI.el('span', { class: 't mono' }, UI.time(e.at)),
                UI.el('span', { class: 'slot mono' }, e.slot),
                UI.el('span', { class: 'plate' }, e.plate || ''),
                UI.el('span', { class: 'transition' },
                    UI.el('span', { class: 'badge ' + (isEntry ? 'in' : isExit ? 'out' : 'note') }, e.transition))
            ]));
        });
        feedPanel.append(feed);

        const alertPanel = document.getElementById('dash-alerts');
        UI.clear(alertPanel);
        alertPanel.append(UI.el('h3', {}, 'Capacity alerts'));
        API.get('/api/system/alerts?limit=6').then(alerts => {
            if (!alerts.length) {
                alertPanel.append(UI.el('div', { class: 'muted small' }, 'No alerts — capacity healthy.'));
                return;
            }
            alerts.forEach(a => {
                alertPanel.append(UI.el('div', { class: 'feed-row' }, [
                    UI.el('span', { class: 't mono' }, UI.time(a.at)),
                    UI.el('span', { class: 'mono' }, a.code),
                    UI.el('span', {}, a.message),
                    UI.el('span', { class: 'transition' },
                        UI.el('span', { class: 'badge ' + (a.active ? 'in' : 'out') }, a.active ? 'ACTIVE' : 'cleared'))
                ]));
            });
        }).catch(() => {
            alertPanel.append(UI.el('div', { class: 'muted small' }, 'Alerts unavailable (ADMIN view).'));
        });
    }

    Store.onChange(render);
    render();
};
