package com.parksense.hardware.spi;

import java.time.Instant;

/**
 * Normalised output of a licence-plate read: whatever the camera vendor
 * calls things, this is the single shape the rest of ParkSense consumes.
 */
public record PlateScan(String plateNo, double confidence, String cameraId, Instant readAt) {
}
