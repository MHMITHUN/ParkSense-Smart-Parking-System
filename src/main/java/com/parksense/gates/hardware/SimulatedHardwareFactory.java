package com.parksense.gates.hardware;

import com.parksense.hardware.spi.Barrier;
import com.parksense.hardware.spi.PlateReader;
import com.parksense.hardware.spi.PlateScan;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The demo equipment family: everything runs in-process. The "camera"
 * interprets the operator's input text as the frame content; the barrier
 * is a boolean with a cycle counter. Zero hardware, zero setup — this is
 * the family the browser demo boots with.
 */
public final class SimulatedHardwareFactory extends GateHardwareFactory {

    @Override
    public String familyName() {
        return "SIM-KIT";
    }

    @Override
    public PlateReader createPlateReader(String laneId) {
        return payload -> {
            String plate = payload == null ? "" : payload.trim().toUpperCase().replace(' ', '-');
            return new PlateScan(plate.isEmpty() ? "UNREADABLE" : plate, 0.99,
                    "SIM-CAM/" + laneId, Instant.now());
        };
    }

    @Override
    public Barrier createBarrier(String laneId) {
        return new Barrier() {
            private volatile boolean open;
            private final AtomicLong cycles = new AtomicLong();

            @Override
            public void raise() {
                open = true;
                cycles.incrementAndGet();
            }

            @Override
            public void lower() {
                open = false;
                cycles.incrementAndGet();
            }

            @Override
            public boolean isOpen() {
                return open;
            }
        };
    }
}
