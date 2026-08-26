package com.parksense.store;

import com.parksense.tariff.TariffKind;
import com.parksense.tariff.TariffPlan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory home of tariff plans. At most one plan per kind may be active —
 * {@link #save} enforces mutual exclusion so the selector can never face
 * two competing active plans of the same kind.
 */
public final class TariffStore {

    private final Map<String, TariffPlan> byId = new ConcurrentHashMap<>();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    /** Persistence hook: called after every mutation. */
    public void onChange(Runnable listener) {
        changeListeners.add(listener);
    }

    private void fireChange() {
        changeListeners.forEach(Runnable::run);
    }

    public void save(TariffPlan plan) {
        if (plan.active()) {
            byId.values().stream()
                    .filter(other -> other.kind() == plan.kind() && other.active()
                            && !other.id().equals(plan.id()))
                    .forEach(TariffPlan::deactivate);
        }
        byId.put(plan.id(), plan);
        fireChange();
    }

    public Optional<TariffPlan> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<TariffPlan> activeByKind(TariffKind kind) {
        return byId.values().stream()
                .filter(p -> p.kind() == kind && p.active())
                .findFirst();
    }

    public List<TariffPlan> all() {
        List<TariffPlan> plans = new ArrayList<>(byId.values());
        plans.sort((a, b) -> a.kind().compareTo(b.kind()));
        return plans;
    }
}
