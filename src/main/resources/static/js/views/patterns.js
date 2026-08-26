/* Patterns guide — the 16 GoF patterns this system implements. */
Views.patterns = function (root) {
    root.append(UI.el('div', { class: 'page-head' }, [
        UI.el('h1', {}, 'Design Patterns Guide'),
        UI.el('p', {}, 'All 16 GoF patterns implemented in ParkSense — intent, classes and the user flow that exercises each')
    ]));

    const holder = UI.el('div');
    root.append(holder);

    const ORDER = ['Creational', 'Structural', 'Behavioral'];

    API.get('/api/system/patterns').then(patterns => {
        ORDER.forEach(cat => {
            const inCat = patterns.filter(p => p.category === cat);
            if (!inCat.length) return;
            const section = UI.el('div', { class: 'pattern-cat' }, [
                UI.el('h2', {}, cat + ' (' + inCat.length + ')'),
                UI.el('div', { class: 'pattern-grid' })
            ]);
            const grid = section.querySelector('.pattern-grid');
            inCat.forEach(p => {
                grid.append(UI.el('div', { class: 'pattern-card' }, [
                    UI.el('div', { class: 'p-name' }, p.name),
                    UI.el('div', { class: 'p-intent' }, p.intent),
                    UI.el('div', { class: 'p-classes' },
                        p.classes.map(c => UI.el('code', {}, c))),
                    UI.el('div', { class: 'p-flow' }, p.flow)
                ]));
            });
            holder.append(section);
        });
    }).catch(e => {
        holder.append(UI.el('div', { class: 'panel' }, 'Failed to load: ' + e.error));
    });
};
