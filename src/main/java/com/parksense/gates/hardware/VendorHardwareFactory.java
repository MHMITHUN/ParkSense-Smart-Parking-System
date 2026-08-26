package com.parksense.gates.hardware;

import com.parksense.hardware.adapter.ParktronBarrierAdapter;
import com.parksense.hardware.adapter.PlateSenseAdapter;
import com.parksense.hardware.spi.Barrier;
import com.parksense.hardware.spi.PlateReader;
import com.parksense.hardware.vendor.ParktronBarrierGate;
import com.parksense.hardware.vendor.PlateSenseAnprCamera;

/**
 * The production equipment family: every device is a vendor part wrapped
 * in an adapter. Swapping the whole site from simulation to real hardware
 * is a one-line change — this factory instead of
 * {@code SimulatedHardwareFactory} — because of the Factory Method seam.
 */
public final class VendorHardwareFactory extends GateHardwareFactory {

    @Override
    public String familyName() {
        return "VENDOR";
    }

    @Override
    public PlateReader createPlateReader(String laneId) {
        return new PlateSenseAdapter(new PlateSenseAnprCamera("FX200"));
    }

    @Override
    public Barrier createBarrier(String laneId) {
        return new ParktronBarrierAdapter(new ParktronBarrierGate());
    }
}
