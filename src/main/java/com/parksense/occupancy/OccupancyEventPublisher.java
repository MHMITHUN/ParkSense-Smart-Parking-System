package com.parksense.occupancy;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Fan-out hub for {@link OccupancyEvent}s (GoF Observer — the Subject side).
 * Subscribers may register at any time; publication never blocks on a slow
 * or failing listener.
 */
public final class OccupancyEventPublisher {

    private final CopyOnWriteArrayList<OccupancyListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(OccupancyListener listener) {
        listeners.addIfAbsent(listener);
    }

    public void unsubscribe(OccupancyListener listener) {
        listeners.remove(listener);
    }

    public void publish(OccupancyEvent event) {
        for (OccupancyListener listener : listeners) {
            try {
                listener.onOccupancyEvent(event);
            } catch (RuntimeException suppressed) {
                // one broken board must never stop the other subscribers
            }
        }
    }

    public int subscriberCount() {
        return listeners.size();
    }
}
