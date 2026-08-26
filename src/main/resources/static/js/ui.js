/* ParkSense UI helpers. */
const Views = {};   // view registry — declared here (loads before the view files)

const UI = {

    /** Create an element: el('div', {class: 'x', onclick: fn}, [children]) */
    el(tag, attrs, children) {
        const node = document.createElement(tag);
        if (attrs) {
            for (const [k, v] of Object.entries(attrs)) {
                if (k === 'class') node.className = v;
                else if (k.startsWith('on') && typeof v === 'function') node.addEventListener(k.slice(2), v);
                else if (v !== null && v !== undefined) node.setAttribute(k, v);
            }
        }
        const kids = Array.isArray(children) ? children : (children === undefined ? [] : [children]);
        for (const kid of kids) {
            if (kid === null || kid === undefined) continue;
            node.append(kid.nodeType ? kid : document.createTextNode(String(kid)));
        }
        return node;
    },

    clear(node) { while (node.firstChild) node.removeChild(node.firstChild); },

    money(v) {
        const n = Number(v || 0);
        return n.toLocaleString('en-BD', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    },

    time(iso) {
        if (!iso) return '—';
        return new Date(iso).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    },

    dateTime(iso) {
        if (!iso) return '—';
        return new Date(iso).toLocaleString('en-GB', {
            day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit'
        });
    },

    ago(iso) {
        if (!iso) return '';
        const mins = Math.max(0, Math.floor((Date.now() - new Date(iso)) / 60000));
        if (mins < 60) return mins + 'm ago';
        const h = Math.floor(mins / 60);
        if (h < 24) return h + 'h ' + (mins % 60) + 'm ago';
        return Math.floor(h / 24) + 'd ago';
    },

    toast(message, kind) {
        const box = document.getElementById('toasts');
        const t = UI.el('div', { class: 'toast ' + (kind || '') }, message);
        box.append(t);
        setTimeout(() => { t.style.opacity = '0'; t.style.transition = 'opacity .3s'; }, 3400);
        setTimeout(() => t.remove(), 3800);
    },

    /** Persistent server-offline banner: shows on network failure, hides on success. */
    setOffline(off) {
        let bar = document.getElementById('offline-banner');
        if (off) {
            if (!bar) {
                bar = UI.el('div', { id: 'offline-banner' }, [
                    UI.el('span', { class: 'pulse-dot' }),
                    'Server unreachable — start it with ',
                    UI.el('code', {}, '.\\mvnw.cmd spring-boot:run'),
                    ' then refresh.'
                ]);
                document.body.prepend(bar);
            }
        } else if (bar) {
            bar.remove();
        }
    },

    /**
     * Animate a number from 0 (or the current text) up to the target —
     * the KPI tiles' count-up. Renders via formatter so money/ints both work.
     */
    countUp(node, target, formatter) {
        if (!node) return;
        const format = formatter || (v => String(Math.round(v)));
        const duration = 650;
        const start = performance.now();
        const from = 0;
        function frame(now) {
            const t = Math.min(1, (now - start) / duration);
            const eased = 1 - Math.pow(1 - t, 3); // easeOutCubic
            node.textContent = format(from + (target - from) * eased);
            if (t < 1) requestAnimationFrame(frame);
        }
        requestAnimationFrame(frame);
    },

    /** Shimmer skeleton block (loading placeholder). */
    skeleton(cls) {
        return UI.el('div', { class: 'skeleton ' + (cls || '') });
    },

    modal(title, bodyNode, actions) {
        const root = document.getElementById('modal-root');
        UI.clear(root);
        const close = () => UI.clear(root);
        const m = UI.el('div', { class: 'modal-back', onclick: (e) => { if (e.target === m) close(); } }, [
            UI.el('div', { class: 'modal' }, [
                UI.el('h3', {}, title),
                bodyNode,
                UI.el('div', { class: 'modal-actions' },
                    (actions || []).map(a =>
                        UI.el('button', {
                            class: 'btn ' + (a.kind ? a.kind : 'secondary'),
                            onclick: () => a.onclick(close)
                        }, a.label)))
            ])
        ]);
        root.append(m);
        return close;
    },

    stateBadge(state) {
        const map = {
            FREE: 'out', RESERVED: 'warn', OCCUPIED: 'in', OUT_OF_SERVICE: 'note',
            ACTIVE: 'in', PAID: 'accent', EXITED: 'out', ISSUED: 'warn',
            LOST: 'warn', VOID: 'note'
        };
        return UI.el('span', { class: 'badge ' + (map[state] || 'note') }, state);
    }
};

setInterval(() => {
    const c = document.getElementById('clock');
    if (c) c.textContent = new Date().toLocaleTimeString('en-GB');
}, 1000);
