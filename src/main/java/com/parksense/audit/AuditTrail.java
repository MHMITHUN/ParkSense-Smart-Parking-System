package com.parksense.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Append-only audit trail. Entries can never be edited or removed — the
 * strongest guarantee in the system is that every sensitive action leaves
 * a permanent line here.
 */
public final class AuditTrail {

    private final List<AuditEntry> entries = new CopyOnWriteArrayList<>();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    /** Persistence hook: called after every recorded entry. */
    public void onChange(Runnable listener) {
        changeListeners.add(listener);
    }

    private void fireChange() {
        changeListeners.forEach(Runnable::run);
    }

    public void record(AuditEntry entry) {
        entries.add(entry);
        fireChange();
    }

    public void record(String actor, String action, String detail, boolean allowed) {
        record(new AuditEntry(java.time.Instant.now(), actor, action, detail, allowed));
    }

    /** Newest-first slice. */
    public List<AuditEntry> latest(int count) {
        List<AuditEntry> copy = new ArrayList<>(entries);
        Collections.reverse(copy);
        return copy.stream().limit(count).toList();
    }

    public List<AuditEntry> all() {
        return List.copyOf(entries);
    }

    public int size() {
        return entries.size();
    }
}
