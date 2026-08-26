/* Ticketing & checkout — open tickets, add-ons, payment, receipt. */
Views.tickets = function (root) {
    let selectedTicket = null;
    let filterState = '';
    let searchPlate = '';

    root.append(UI.el('div', { class: 'page-head' }, [
        UI.el('h1', {}, 'Ticketing & Checkout'),
        UI.el('p', {}, 'Settle fares, stack add-on services, print receipts')
    ]));

    const wrap = UI.el('div', { class: 'ticket-wrap' });
    const listPanel = UI.el('div', { class: 'panel' }, [UI.el('h3', {}, 'Open tickets')]);
    const detailPanel = UI.el('div', { class: 'panel' }, [UI.el('h3', {}, 'Ticket detail')]);
    wrap.append(listPanel, detailPanel);
    root.append(wrap);

    async function loadList() {
        UI.clear(listPanel);
        listPanel.append(UI.el('h3', {}, 'Open tickets'));

        const search = UI.el('input', { type: 'text', placeholder: 'Search plate…', value: searchPlate });
        search.addEventListener('input', () => { searchPlate = search.value; drawList(); });

        const chips = UI.el('div', { class: 'chip-row mt' });
        [['', 'All'], ['ACTIVE', 'Unpaid'], ['PAID', 'Paid'], ['LOST', 'Lost'], ['RESERVED-ish', '—']].slice(0, 4).forEach(([v, label]) => {
            chips.append(UI.el('span', {
                class: 'chip' + (filterState === v ? ' on' : ''),
                onclick: () => { filterState = v; loadList(); }
            }, label));
        });

        listPanel.append(search, chips);

        let tickets;
        try {
            tickets = await API.get('/api/tickets?limit=200');
        } catch (e) { listPanel.append(UI.el('div', { class: 'muted small' }, e.error)); return; }

        const filtered = tickets
            .filter(t => t.state !== 'EXITED' && t.state !== 'VOID')
            .filter(t => !filterState || t.state === filterState)
            .filter(t => !searchPlate || (t.plate || '').toUpperCase().includes(searchPlate.toUpperCase()));

        const holder = UI.el('div', { class: 'mt' });
        if (!filtered.length) holder.append(UI.el('div', { class: 'muted small' }, 'No open tickets.'));
        filtered.slice(0, 40).forEach(t => {
            holder.append(UI.el('div', {
                class: 'tk-row' + (selectedTicket === t.ticketNo ? ' selected' : ''),
                onclick: () => { selectedTicket = t.ticketNo; loadDetail(t.ticketNo); loadList(); }
            }, [
                UI.el('div', {}, [
                    UI.el('div', { class: 'tkno' }, t.ticketNo),
                    UI.el('div', { class: 'plate muted small' }, t.plate)
                ]),
                UI.el('span', { class: 'st' }, UI.stateBadge(t.state))
            ]));
        });
        listPanel.append(holder);
    }

    async function loadDetail(ticketNo) {
        let t;
        try { t = await API.get('/api/tickets/' + ticketNo); } catch (e) { UI.toast(e.error, 'err'); return; }
        UI.clear(detailPanel);
        detailPanel.append(UI.el('h3', {}, 'Ticket ' + t.ticketNo));

        detailPanel.append(UI.el('div', { class: 'row wrap' }, [
            UI.stateBadge(t.state),
            UI.el('span', { class: 'badge note' }, t.vehicleType),
            t.accessible ? UI.el('span', { class: 'badge warn' }, '♿ accessible') : null
        ]));

        detailPanel.append(UI.el('div', { class: 'fee-lines' }, [
            UI.el('div', { class: 'fee-line' }, ['Plate', UI.el('span', { class: 'amt mono' }, t.plate)]),
            UI.el('div', { class: 'fee-line' }, ['Bay', UI.el('span', { class: 'amt mono' }, t.slot)]),
            UI.el('div', { class: 'fee-line' }, ['Entered', UI.el('span', { class: 'amt' }, UI.dateTime(t.entryTime) + ' (' + UI.ago(t.entryTime) + ')')]),
            UI.el('div', { class: 'fee-line' }, ['Tariff', UI.el('span', { class: 'amt' }, t.tariffExplain || '—')])
        ]));

        if (t.state === 'ACTIVE' || t.state === 'ISSUED') {
            const addonRow = UI.el('div', { class: 'addon-row' }, [
                UI.el('span', { class: 'muted small' }, 'Add-ons:')
            ]);
            const has = code => (t.feeLines || []).some(l => l[0].toLowerCase().includes(code));
            [['CAR_WASH', '🚿 Car wash ৳120', 'wash'], ['VALET', '🤵 Valet ৳200', 'valet'], ['EV_CHARGE', '⚡ EV top-up ৳407', 'ev top-up']].forEach(([code, label, match]) => {
                const on = has(match);
                addonRow.append(UI.el('button', {
                    class: 'btn small ' + (on ? '' : 'secondary'),
                    onclick: async () => {
                        try {
                            const r = await API.post(`/api/tickets/${t.ticketNo}/addons`, { code });
                            UI.toast(on ? 'Add-on removed' : 'Add-on added: ' + code, 'ok');
                            loadDetail(t.ticketNo);
                        } catch (e) { UI.toast(e.error, 'err'); }
                    }
                }, (on ? '✓ ' : '+ ') + label));
            });
            detailPanel.append(addonRow);

            detailPanel.append(UI.el('button', {
                class: 'btn mt',
                onclick: () => payModal(t)
            }, '💳 Pay & settle'));
            detailPanel.append(' ');
            detailPanel.append(UI.el('button', {
                class: 'btn secondary mt',
                onclick: async () => {
                    try { await API.post(`/api/tickets/${t.ticketNo}/lost`); UI.toast('Marked LOST — penalty tariff applies', 'ok'); loadDetail(t.ticketNo); loadList(); }
                    catch (e) { UI.toast(e.error, 'err'); }
                }
            }, 'Mark lost'));
            if (API.isAdmin()) {
                detailPanel.append(' ');
                detailPanel.append(UI.el('button', {
                    class: 'btn danger mt',
                    onclick: async () => {
                        try { await API.post(`/api/tickets/${t.ticketNo}/void`, { reason: 'admin void from checkout' }); UI.toast('Ticket voided', 'ok'); loadList(); UI.clear(detailPanel); detailPanel.append(UI.el('h3', {}, 'Ticket detail')); }
                        catch (e) { UI.toast(e.error, 'err'); }
                    }
                }, 'Void (admin)'));
            }
        } else if (t.state === 'LOST') {
            detailPanel.append(UI.el('button', { class: 'btn mt', onclick: () => payModal(t) }, '💳 Settle penalty'));
        }

        if (t.feeLines && t.feeLines.length) {
            const box = UI.el('div', { class: 'mt' });
            box.append(UI.el('h3', {}, 'Fee breakdown'));
            t.feeLines.forEach((l, i) => box.append(UI.el('div', {
                class: 'fee-line',
                style: 'animation-delay:' + (i * 70) + 'ms'
            }, [l[0], UI.el('span', { class: 'amt' }, '৳ ' + UI.money(l[1]))])));
            box.append(UI.el('div', { class: 'fee-line total' },
                ['Total', UI.el('span', { class: 'amt' }, '৳ ' + UI.money(t.feeTotal))]));
            detailPanel.append(box);
        }
    }

    function payModal(t) {
        const method = UI.el('select', {}, [
            UI.el('option', { value: 'CASH' }, 'Cash'),
            UI.el('option', { value: 'CARD' }, 'Card'),
            UI.el('option', { value: 'MOBILE' }, 'Mobile banking')
        ]);
        const tendered = UI.el('input', { type: 'number', placeholder: '৳ tendered', min: '0', step: '10' });
        const body = UI.el('div', { class: 'grid' }, [
            UI.el('label', { class: 'fld' }, ['Method', method]),
            UI.el('label', { class: 'fld' }, ['Tendered (৳)', tendered]),
            UI.el('div', { class: 'muted small' }, 'Leave tendered empty to pay the exact amount.')
        ]);
        UI.modal('Settle ' + t.ticketNo + ' — ৳ ' + UI.money(t.feeTotal), body, [
            { label: 'Cancel' },
            {
                label: 'Confirm payment', kind: '',
                onclick: async (close) => {
                    try {
                        const r = await API.post(`/api/tickets/${t.ticketNo}/pay`, {
                            method: method.value,
                            tendered: tendered.value ? Number(tendered.value) : null
                        });
                        close();
                        receiptModal(r, t);
                        loadList();
                        loadDetail(t.ticketNo);
                    } catch (e) { UI.toast(e.error, 'err'); }
                }
            }
        ]);
    }

    function receiptModal(r, t) {
        const lines = (t.feeLines || []).map(l =>
            UI.el('div', { class: 'rc-row' }, [l[0], UI.el('span', {}, '৳' + UI.money(l[1]))]));
        const receipt = UI.el('div', { class: 'receipt' }, [
            UI.el('div', { class: 'rc-title' }, 'PARKSENSE CENTRAL PLAZA'),
            UI.el('div', { style: 'text-align:center' }, 'PAYMENT RECEIPT'),
            UI.el('div', { class: 'rc-sep' }),
            UI.el('div', { class: 'rc-row' }, ['TICKET', r.ticketNo]),
            UI.el('div', { class: 'rc-row' }, ['PLATE', r.plate]),
            UI.el('div', { class: 'rc-sep' }),
            ...lines,
            UI.el('div', { class: 'rc-row' }, [UI.el('b', {}, 'TOTAL PAID'), UI.el('b', {}, '৳' + UI.money(r.total))]),
            UI.el('div', { class: 'rc-row' }, ['METHOD', r.method]),
            r.change && Number(r.change) > 0 ? UI.el('div', { class: 'rc-row' }, ['CHANGE', '৳' + UI.money(r.change)]) : null,
            UI.el('div', { class: 'rc-sep' }),
            UI.el('div', { style: 'text-align:center' }, 'Exit within the grace window —'),
            UI.el('div', { style: 'text-align:center' }, 'free exit before ' + UI.time(r.graceUntil))
        ]);
        UI.modal('Payment settled', receipt, [{ label: 'Done', kind: '' }]);
    }

    loadList();
    detailPanel.append(UI.el('div', { class: 'muted small' }, 'Select a ticket from the list.'));
};
