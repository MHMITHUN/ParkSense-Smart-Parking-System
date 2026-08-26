/* Reports — visitor-generated tables, charts and CSV export. */
Views.reports = function (root) {
    let current = 'revenue';

    root.append(UI.el('div', { class: 'page-head' }, [
        UI.el('h1', {}, 'Reports'),
        UI.el('p', {}, 'Every report is a design-pattern visitor walking the lot tree or ticket history')
    ]));

    const tabs = UI.el('div', { class: 'chip-row' });
    const from = UI.el('input', { type: 'date', value: new Date(Date.now() - 7 * 864e5).toISOString().slice(0, 10) });
    const to = UI.el('input', { type: 'date', value: new Date().toISOString().slice(0, 10) });
    const runBtn = UI.el('button', { class: 'btn small', onclick: () => render() }, '⟳ Run');
    const csvBtn = UI.el('button', {
        class: 'btn secondary small',
        onclick: () => {
            window.open(`/api/reports/${current}?from=${from.value}&to=${to.value}&format=csv`, '_blank');
        }
    }, '⬇ CSV');
    root.append(tabs);
    root.append(UI.el('div', { class: 'row mt wrap' }, [
        UI.el('label', { class: 'fld', style: 'flex-direction:row;align-items:center;gap:6px' }, ['From', from]),
        UI.el('label', { class: 'fld', style: 'flex-direction:row;align-items:center;gap:6px' }, ['To', to]),
        runBtn,
        csvBtn
    ]));

    const out = UI.el('div', { class: 'panel mt' });
    root.append(out);

    const REPORTS = [
        ['revenue', 'Revenue by day'], ['occupancy', 'Occupancy by zone'],
        ['utilization', 'Utilization by floor'], ['peak-hours', 'Peak exit hours'],
        ['addons', 'Add-on services']
    ];

    function drawTabs() {
        UI.clear(tabs);
        REPORTS.forEach(([id, label]) => {
            tabs.append(UI.el('span', {
                class: 'chip' + (current === id ? ' on' : ''),
                onclick: () => { current = id; drawTabs(); render(); }
            }, label));
        });
    }

    async function render() {
        UI.clear(out);
        out.append(UI.el('h3', {}, 'Running ' + current + '…'));
        let r;
        try {
            r = await API.get(`/api/reports/${current}?from=${from.value}&to=${to.value}`);
        } catch (e) {
            UI.clear(out);
            out.append(UI.el('h3', {}, current), UI.el('div', { class: 'muted' }, e.error));
            return;
        }
        UI.clear(out);
        out.append(UI.el('h3', {}, r.name + '  (' + (r.rows ? r.rows.length : 0) + ' rows)'));

        if (r.rows && r.rows.length) {
            const tbl = UI.el('table', { class: 'tbl' });
            tbl.append(UI.el('tr', {}, r.columns.map(c => UI.el('th', {}, c))));
            r.rows.forEach(row => {
                tbl.append(UI.el('tr', {}, row.map((cell, i) =>
                    UI.el('td', { class: /^\d+([.,]\d+)?$/.test(String(cell).replace(/,/g, '')) && i > 0 ? 'num' : '' },
                        String(cell)))));
            });
            out.append(tbl);
        } else {
            out.append(UI.el('div', { class: 'muted small' }, 'No data in this window.'));
        }

        if (r.seriesLabels && r.seriesLabels.length) {
            const max = Math.max(...r.seriesValues, 1);
            const chart = UI.el('div', { class: 'chart mt' });
            r.seriesLabels.forEach((label, i) => {
                const v = r.seriesValues[i];
                chart.append(UI.el('div', { class: 'bar-wrap', title: label + ': ' + v }, [
                    UI.el('span', { class: 'bar-val' }, String(v)),
                    UI.el('div', { class: 'bar', style: 'height:' + Math.max(3, v * 100 / max) + '%' }),
                    UI.el('span', { class: 'bar-lbl' }, label.length > 6 ? label.slice(5) : label)
                ]));
            });
            out.append(chart);
        }
    }

    drawTabs();
    render();
};
