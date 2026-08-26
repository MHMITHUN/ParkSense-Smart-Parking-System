/* Live parking map — 2-column layout with real-time Bay Inspector */
Views.map = function (root) {
    let selectedCode = null;
    let typeFilter = '';
    let stateFilter = '';
    let levelFilter = '';
    let currentData = null;

    /* ── Page header ── */
    root.append(UI.el('div', { class: 'page-head' }, [
        UI.el('h1', {}, 'Live Parking Map'),
        UI.el('p', {}, 'Real-time multi-level lot telemetry — click any bay to inspect sensor status and vehicle details')
    ]));

    /* ── Legend ── */
    const legendItems = [
        { cls: 'free',     bg: '#064e3b', border: 'rgba(52,211,153,.5)', label: 'Available' },
        { cls: 'occupied', bg: '#7f1d1d', border: 'rgba(248,113,113,.5)', label: 'Occupied' },
        { cls: 'reserved', bg: '#78350f', border: 'rgba(251,191,36,.5)',  label: 'Reserved' },
        { cls: 'oos',      bg: '#0f172a', border: 'rgba(71,85,105,.4)',   label: 'Out of service' }
    ];
    const legend = UI.el('div', { class: 'legend' },
        legendItems.map(i => UI.el('div', { class: 'li' }, [
            UI.el('span', { class: 'sw', style: `background:${i.bg}; border:1.5px solid ${i.border}` }),
            i.label
        ]))
    );

    /* ── Filter rows ── */
    const filterWrap = UI.el('div', { style: 'display:flex; flex-direction:column; gap:6px; margin-bottom:16px;' });
    const levelChips = UI.el('div', { class: 'chip-row' });
    const typeChips  = UI.el('div', { class: 'chip-row' });
    const stateChips = UI.el('div', { class: 'chip-row' });
    filterWrap.append(levelChips, typeChips, stateChips);

    /* ── 2-column layout ── */
    const layout    = UI.el('div', { class: 'map-layout' });
    const mapMain   = UI.el('div', { class: 'map-main' });
    const mapSidebar = UI.el('div', { class: 'map-sidebar' });
    layout.append(mapMain, mapSidebar);

    /* ── Inspector card ── */
    const inspCard = UI.el('div', { class: 'inspector-card' });
    const inspHead = UI.el('div', { class: 'inspector-head' }, [
        UI.el('span', { class: 'ih-label' }, 'Bay Inspector'),
        UI.el('span', { class: 'badge note small' }, 'LIVE')
    ]);
    const inspBody = UI.el('div', { class: 'inspector-body' });
    inspCard.append(inspHead, inspBody);
    mapSidebar.append(inspCard);

    root.append(legend, filterWrap, layout);

    /* ── Options ── */
    const LEVELS = [['', 'All Floors'], ['L1', 'Level 1'], ['L2', 'Level 2'], ['L3', 'Level 3']];
    const TYPES  = [
        ['', 'All types'],
        ['STANDARD', 'Standard'], ['COMPACT', 'Compact'],
        ['ACCESSIBLE', 'Accessible'], ['EV_CHARGE', 'EV Charge'],
        ['MOTORCYCLE', 'Motorcycle']
    ];
    const STATES = [
        ['', 'All states'], ['FREE', 'Available'],
        ['OCCUPIED', 'Occupied'], ['RESERVED', 'Reserved'], ['OUT_OF_SERVICE', 'OOS']
    ];

    const GLYPH = { EV_CHARGE: '⚡', ACCESSIBLE: '♿', MOTORCYCLE: 'M', COMPACT: 'C' };
    const STATE_CLS = { FREE: 'free', OCCUPIED: 'occupied', RESERVED: 'reserved', OUT_OF_SERVICE: 'oos' };
    const STATE_BADGE = { FREE: 'out', OCCUPIED: 'in', RESERVED: 'warn', OUT_OF_SERVICE: 'note' };

    /* ── Chip helper ── */
    function renderChips(container, opts, current, onChange) {
        UI.clear(container);
        opts.forEach(([val, label]) => {
            container.append(UI.el('span', {
                class: 'chip' + (current === val ? ' on' : ''),
                onclick: () => { onChange(val); }
            }, label));
        });
    }

    function refreshFilters() {
        renderChips(levelChips, LEVELS, levelFilter, v => { levelFilter = v; refreshFilters(); renderGrid(); });
        renderChips(typeChips,  TYPES,  typeFilter,  v => { typeFilter  = v; refreshFilters(); renderGrid(); });
        renderChips(stateChips, STATES, stateFilter, v => { stateFilter = v; refreshFilters(); renderGrid(); });
    }
    refreshFilters();

    /* ── Inspector renderer ── */
    function renderInspector(slot) {
        UI.clear(inspBody);

        if (!slot) {
            inspBody.append(UI.el('div', { class: 'inspector-empty' }, [
                UI.el('div', { class: 'ei-icon' }, '🅿'),
                UI.el('div', {}, [
                    UI.el('strong', {}, 'No Bay Selected'),
                    'Click any parking bay on the map to view live sensor data, vehicle plate, and ticket details.'
                ])
            ]));
            return;
        }

        const isFree = slot.state === 'FREE';

        const idRow = UI.el('div', { class: 'insp-slot-id' }, [
            UI.el('span', { class: 'icode' }, slot.code),
            UI.el('span', { class: 'itype' }, slot.type.replace('_', ' ')),
            UI.el('span', { class: 'badge ' + (STATE_BADGE[slot.state] || 'note'), style: 'margin-left:auto' },
                isFree ? 'AVAILABLE' : slot.state)
        ]);

        const rows = [
            ['Sensor Node',    'SEN-' + slot.code.replace(/-/g, ''), 'mono'],
            ['Vehicle Plate',  slot.plate      || (isFree ? '—' : 'Unknown'), slot.plate ? 'mono' : ''],
            ['Active Ticket',  slot.ticketNo   || '—', 'mono small'],
            ['Occupied Since', slot.occupiedSince
                ? (UI.dateTime ? UI.dateTime(slot.occupiedSince) : new Date(slot.occupiedSince).toLocaleTimeString())
                : '—', 'small']
        ];

        const details = UI.el('div', { class: 'fee-lines' },
            rows.map(([lbl, val, cls]) =>
                UI.el('div', { class: 'fee-line' }, [
                    lbl,
                    UI.el('span', { class: 'amt ' + cls,
                        style: lbl === 'Vehicle Plate' && slot.plate ? 'color:var(--accent); font-weight:700' : '' },
                        val)
                ])
            )
        );

        const actions = UI.el('div', { style: 'margin-top:14px' });
        if (slot.ticketNo) {
            actions.append(UI.el('a', {
                href: '#/tickets',
                class: 'btn secondary small',
                style: 'width:100%; justify-content:center'
            }, 'View Ticket →'));
        } else if (isFree) {
            actions.append(UI.el('p', {
                class: 'muted small',
                style: 'text-align:center; padding: 6px 0'
            }, 'Bay is vacant and ready for incoming vehicles.'));
        }

        inspBody.append(idRow, details, actions);
    }

    renderInspector(null);

    /* ── Grid renderer ── */
    function renderGrid() {
        if (!currentData) return;
        UI.clear(mapMain);

        const floors = currentData.floors.filter(f => !levelFilter || f.code === levelFilter);

        floors.forEach(floor => {
            const fBlock = UI.el('div', { class: 'floor-block' });
            fBlock.append(UI.el('div', { class: 'floor-head' }, [
                UI.el('span', { class: 'fcode' }, floor.code),
                UI.el('span', { class: 'fname' }, floor.label),
                UI.el('span', { class: 'fcount' }, [
                    UI.el('b', {}, String(floor.freeSlots)),
                    ' / ' + floor.totalSlots + ' available'
                ])
            ]));

            floor.zones.forEach(zone => {
                const zBlock = UI.el('div', { class: 'zone-block' });
                const zoneName = zone.label.includes('—')
                    ? zone.label.split('—').slice(1).join('—').trim() : zone.label;

                zBlock.append(UI.el('div', { class: 'zone-label' }, [
                    UI.el('span', {}, [UI.el('b', {}, zone.code), ' · ' + zoneName]),
                    UI.el('span', { class: 'zfree' }, zone.freeSlots + ' / ' + zone.totalSlots + ' free')
                ]));

                const grid = UI.el('div', { class: 'slot-grid' });

                zone.slots.forEach(slot => {
                    if (typeFilter  && slot.type  !== typeFilter)  return;
                    if (stateFilter && slot.state !== stateFilter) return;

                    const isSelected = selectedCode === slot.code;
                    const cls = 'slot ' + (STATE_CLS[slot.state] || 'oos') + (isSelected ? ' selected' : '');
                    const shortId = slot.code.split('-')[2] || slot.code;

                    const el = UI.el('div', {
                        class: cls,
                        title: slot.code + ' · ' + slot.type + ' · ' + slot.state
                            + (slot.plate ? ' · ' + slot.plate : ''),
                        onclick() {
                            // deselect old
                            mapMain.querySelectorAll('.slot.selected')
                                   .forEach(s => s.classList.remove('selected'));
                            // mark new
                            el.classList.add('selected');
                            selectedCode = slot.code;
                            renderInspector(slot);
                        }
                    }, [
                        shortId,
                        GLYPH[slot.type] ? UI.el('span', { class: 'glyph' }, GLYPH[slot.type]) : null
                    ]);

                    grid.append(el);
                });

                zBlock.append(grid);
                fBlock.append(zBlock);
            });

            mapMain.append(fBlock);
        });
    }

    /* ── Data fetch ── */
    async function load(isFirst) {
        try {
            const data = await API.get('/api/map');
            currentData = data;
            renderGrid();

            // Refresh inspector if a slot was selected
            if (selectedCode) {
                let fresh = null;
                outer: for (const f of data.floors) {
                    for (const z of f.zones) {
                        for (const s of z.slots) {
                            if (s.code === selectedCode) { fresh = s; break outer; }
                        }
                    }
                }
                if (fresh) renderInspector(fresh);
            }
        } catch (e) {
            if (isFirst) UI.toast('Map load failed: ' + (e.error || e), 'err');
        }
    }

    /* Loading state */
    mapMain.append(UI.el('div', { class: 'muted small', style: 'padding:8px 0' }, 'Loading live lot map…'));
    load(true);

    /* Background polling — smooth, no DOM wipe */
    Store.onChange(() => load(false));
};
