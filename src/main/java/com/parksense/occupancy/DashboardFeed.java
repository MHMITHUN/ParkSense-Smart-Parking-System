package com.parksense.occupancy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Rolling window of the newest occupancy events, served to the control
 * room dashboard (GoF Observer subscriber). The web SPA polls this feed
 * and shows entries and exits the moment they happen.
 */
public final class DashboardFeed implements OccupancyListener {

    private final int capacity;
    private final Deque<OccupancyEvent> recent = new ArrayDeque<>();

    public DashboardFeed(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public synchronized void onOccupancyEvent(OccupancyEvent event) {
        recent.addFirst(event);
        while (recent.size() > capacity) {
            recent.removeLast();
        }
    }

    /** Newest-first slice of the feed. */
    public synchronized List<OccupancyEvent> latest(int count) {
        return recent.stream().limit(count).toList();
    }

    public synchronized int size() {
        return recent.size();
    }

    public synchronized List<OccupancyEvent> all() {
        return new ArrayList<>(recent);
    }

    public synchronized void clear() {
        recent.clear();
    }
}
