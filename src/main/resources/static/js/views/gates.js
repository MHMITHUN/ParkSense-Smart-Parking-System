/* Gate simulator — entry/exit lanes with ANPR, chain verdict, command queue. */
Views.gates = function (root) {
    root.append(UI.el('div', { class: 'page-head' }, [
        UI.el('h1', {}, 'Gate Simulator'),
        UI.el('p', {}, 'Drive vehicles through the lanes — every rule, command and barrier movement is visible')
    ]));
    const grid = UI.el('div', { class: 'gate-grid' });
    root.append(grid);

    const VTYPES = ['CAR', 'SUV', 'VAN', 'MOTORCYCLE', 'EV'];

    function traceBox(trace, steps) {
        const box = UI.el('div', { class: 'trace' });
        const ICON = { pass: '✓', fail: '✗', note: '!', step: '›' };
        (trace || []).forEach(line => {
            const kind = line.startsWith('PASS') ? 'pass' : line.startsWith('FAIL') ? 'fail'
                : line.startsWith('NOTE') ? 'note' : 'step';
            box.append(UI.el('div', { class: 'tr-line ' + kind }, [
                UI.el('span', { style: 'width:12px;flex:none;font-weight:700' }, ICON[kind]),
                line
            ]));
        });
        (steps || []).forEach(line => box.append(UI.el('div', { class: 'tr-line step' }, [
            UI.el('span', { style: 'width:12px;flex:none;font-weight:700' }, '›'),
            line
        ])));
        if (!trace && !steps) box.append(UI.el('div', { class: 'tr-line' }, 'no run yet'));
        return box;
    }

    function cmdList(gate) {
        const wrap = UI.el('div', { class: 'cmd-list' });
        gate.commands.slice(0, 8).forEach(c => {
            wrap.append(UI.el('div', { class: 'cmd-row' }, [
                UI.el('span', { class: 'cd mono' }, UI.time(c.at)),
                UI.el('span', { class: 'cdx mono' }, '#' + c.id),
                UI.el('span', {}, c.describe + (c.undone ? ' (undone)' : '')),
                c.undoSupported && !c.undone
                    ? UI.el('button', {
                        class: 'btn secondary small undo',
                        onclick: async () => {
                            try {
                                const r = await API.post(`/api/gates/${gate.id}/commands/${c.id}/undo`);
                                UI.toast(r.undone ? 'Undone: ' + c.describe : 'Cannot undo', r.undone ? 'ok' : 'err');
                                render();
                            } catch (e) { UI.toast(e.error, 'err'); }
                        }
                    }, 'Undo')
                    : null
            ]));
        });
        if (!gate.commands.length) wrap.append(UI.el('div', { class: 'muted small' }, 'queue empty'));
        return wrap;
    }

    async function sendEntry(gate, plate, type, accessible, display, traceEl) {
        try {
            const r = await API.post(`/api/gates/${gate.id}/entry`,
                { plate, vehicleType: type, accessible });
            display.textContent = r.line;
            display.classList.toggle('err', !r.accepted);
            const box = traceEl;
            UI.clear(box);
            box.append(traceBox(r.trace, null));
            if (r.accepted) {
                const slotInfo = r.slotCode ? ' → Slot ' + r.slotCode : '';
                const ticketInfo = r.ticketNo ? ' (Ticket: ' + r.ticketNo + ')' : '';
                UI.toast('Entry Approved: ' + plate + slotInfo + ticketInfo, 'ok');
            } else {
                UI.toast('Entry Refused: ' + (r.line || 'Check vehicle status'), 'err');
            }
            render();
        } catch (e) {
            UI.toast(e.error, 'err');
        }
    }

    async function sendExit(gate, ref, method, tendered, lost, display, traceEl) {
        try {
            const r = await API.post(`/api/gates/${gate.id}/exit`,
                { reference: ref, method, tendered, lostTicket: lost });
            display.textContent = r.line;
            display.classList.toggle('err', !r.allowed);
            UI.clear(traceEl);
            UI.el('div');
            r.steps.forEach(s => traceEl.append(UI.el('div', { class: 'tr-line step' }, s)));
            UI.toast(r.allowed ? 'Exit complete — ' + r.line : 'Exit refused: ' + r.line,
                r.allowed ? 'ok' : 'err');
            render();
        } catch (e) {
            UI.toast(e.error, 'err');
        }
    }

    async function render() {
        let gates;
        try { gates = await API.get('/api/gates'); } catch (e) { UI.toast(e.error, 'err'); return; }
        UI.clear(grid);
        gates.forEach(gate => {
            const display = UI.el('div', { class: 'gate-display' }, gate.display);
            const traceEl = UI.el('div', { class: 'trace' });
            traceEl.append(UI.el('div', { class: 'tr-line' }, 'no run yet'));

            const form = UI.el('div', { class: 'gate-form' });
            if (gate.direction === 'ENTRY') {
                const plate = UI.el('input', { type: 'text', placeholder: 'Plate e.g. DHAKA METRO GA 11-2233', value: 'DHAKA METRO GA 11-2233' });
                const type = UI.el('select', {}, VTYPES.map(t => UI.el('option', { value: t }, t)));
                const acc = UI.el('input', { type: 'checkbox', id: gate.id + '-acc' });
                const accLabel = UI.el('label', { style: 'display:flex;gap:5px;align-items:center;font-size:12px;color:var(--muted);cursor:pointer' }, [acc, 'Accessible slot']);
                const go = UI.el('button', { class: 'btn btn-anpr', title: 'Trigger ANPR camera scan for vehicle entry' });
                go.innerHTML = `<svg style="width:14px;height:14px;margin-right:6px;vertical-align:-2px" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path><circle cx="12" cy="13" r="4"></circle></svg>Trigger ANPR`;
                go.addEventListener('click', () => sendEntry(gate, plate.value, type.value, acc.checked, display, traceEl));
                form.append(plate, type, accLabel, go);
            } else {
                const ref = UI.el('input', { type: 'text', placeholder: 'Ticket no. or license plate', value: '' });
                const method = UI.el('select', {}, [
                    UI.el('option', { value: 'CASH' }, 'Cash Payment'),
                    UI.el('option', { value: 'CARD' }, 'Card Payment'),
                    UI.el('option', { value: 'MOBILE' }, 'Mobile MFS')
                ]);
                const tendered = UI.el('input', { type: 'number', placeholder: '৳ tendered', min: '0', step: '10' });
                const go = UI.el('button', { class: 'btn', title: 'Calculate tariff and process exit payment' });
                go.innerHTML = `<svg style="width:14px;height:14px;margin-right:6px;vertical-align:-2px" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="16" rx="2"></rect><line x1="7" y1="8" x2="17" y2="8"></line><line x1="7" y1="12" x2="17" y2="12"></line><line x1="7" y1="16" x2="13" y2="16"></line></svg>Process Exit`;
                const lostBtn = UI.el('button', { class: 'btn danger', title: 'Issue lost ticket penalty & exit' }, 'Lost Ticket');
                go.addEventListener('click', () =>
                    sendExit(gate, ref.value, method.value, tendered.value ? Number(tendered.value) : null, false, display, traceEl));
                lostBtn.addEventListener('click', () =>
                    sendExit(gate, ref.value, 'CASH', tendered.value ? Number(tendered.value) : null, true, display, traceEl));
                form.append(ref, method, tendered, go, lostBtn);
            }

            const forceRow = UI.el('div', { class: 'row mt' }, [
                UI.el('span', { class: 'muted small' }, 'Emergency:'),
                UI.el('button', {
                    class: 'btn danger small',
                    onclick: async () => {
                        try { await API.post(`/api/control/gates/${gate.id}/force-open`); UI.toast('Barrier forced OPEN', 'ok'); render(); }
                        catch (e) { UI.toast('Proxy: ' + e.error, 'err'); }
                    }
                }, 'Force open'),
                UI.el('button', {
                    class: 'btn danger small',
                    onclick: async () => {
                        try { await API.post(`/api/control/gates/${gate.id}/force-close`); UI.toast('Barrier forced CLOSED', 'ok'); render(); }
                        catch (e) { UI.toast('Proxy: ' + e.error, 'err'); }
                    }
                }, 'Force close')
            ]);

            grid.append(UI.el('div', { class: 'gate-panel' }, [
                UI.el('div', { class: 'gate-head' }, [
                    UI.el('span', { class: 'gcode mono' }, gate.code),
                    UI.el('span', { class: 'badge ' + (gate.direction === 'ENTRY' ? 'accent' : 'out') }, gate.direction),
                    UI.el('span', { class: 'badge note' }, gate.laneKind),
                    UI.el('span', { class: 'spacer' }),
                    UI.el('span', { class: 'hw mono' }, gate.hardwareFamily)
                ]),
                display,
                UI.el('div', { class: 'barrier' + (gate.barrierOpen ? ' open' : '') }, [
                    UI.el('div', { class: 'post' }),
                    UI.el('div', { class: 'arm-holder' }, [UI.el('div', { class: 'arm' })]),
                    UI.el('span', { class: 'state' }, gate.barrierOpen ? 'arm raised' : 'arm lowered')
                ]),
                form,
                gate.direction === 'EXIT' ? forceRow : null,
                traceEl,
                UI.el('h3', { style: 'margin:14px 0 4px;font-size:11px;letter-spacing:1.4px;text-transform:uppercase;color:var(--muted)' }, 'Command queue'),
                cmdList(gate)
            ]));
        });
    }

    render();
};
