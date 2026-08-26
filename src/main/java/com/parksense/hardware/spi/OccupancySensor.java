package com.parksense.hardware.spi;

/**
 * Internal port for slot-presence sensing.
 */
public interface OccupancySensor {

    /** Poll one slot's sensor node. */
    SlotSignal poll(String slotCode);
}
