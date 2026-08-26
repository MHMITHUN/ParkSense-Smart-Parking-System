package com.parksense.hardware.adapter;

import com.parksense.hardware.spi.PlateReader;
import com.parksense.hardware.spi.PlateScan;
import com.parksense.hardware.vendor.PlateSenseAnprCamera;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * GoF Adapter: makes the PlateSense FX-200 camera speak ParkSense's
 * {@code PlateReader} port. Vendor struct → domain record, epoch millis →
 * Instant, percent → fraction, plate text normalised.
 */
public final class PlateSenseAdapter implements PlateReader {

    private final PlateSenseAnprCamera camera;

    public PlateSenseAdapter(PlateSenseAnprCamera camera) {
        this.camera = camera;
    }

    @Override
    public PlateScan read(String rawCameraPayload) {
        PlateSenseAnprCamera.VendorScan vendor = camera.pushFrame(
                rawCameraPayload.getBytes(StandardCharsets.UTF_8), "LANE");
        return new PlateScan(
                vendor.plate_text(),
                vendor.confidence_pct() / 100.0,
                vendor.device_tag(),
                Instant.ofEpochMilli(vendor.ts_epoch_ms()));
    }
}
