package com.parksense.occupancy;

/**
 * Observer interface (GoF Observer). Any component that must react to slot
 * state changes implements this and subscribes with the
 * {@code OccupancyEventPublisher}; publishers and subscribers never learn
 * about each other's concrete types.
 */
@FunctionalInterface
public interface OccupancyListener {

    void onOccupancyEvent(OccupancyEvent event);
}
