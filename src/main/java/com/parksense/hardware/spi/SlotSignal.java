package com.parksense.hardware.spi;

import java.time.Instant;

/**
 * Normalised slot-presence reading from the sensor network.
 */
public record SlotSignal(String slotCode, boolean vehiclePresent, Instant signalAt) {
}
