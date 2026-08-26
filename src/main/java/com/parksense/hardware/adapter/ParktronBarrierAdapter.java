package com.parksense.hardware.adapter;

import com.parksense.hardware.spi.Barrier;
import com.parksense.hardware.vendor.ParktronBarrierGate;

/**
 * GoF Adapter: ParkSense's raise/lower barrier port on top of the
 * Parktron BG-12 motor controller's command-word API.
 */
public final class ParktronBarrierAdapter implements Barrier {

    private final ParktronBarrierGate gate;

    public ParktronBarrierAdapter(ParktronBarrierGate gate) {
        this.gate = gate;
    }

    @Override
    public void raise() {
        gate.sendWord(ParktronBarrierGate.CMD_RAISE);
    }

    @Override
    public void lower() {
        gate.sendWord(ParktronBarrierGate.CMD_LOWER);
    }

    @Override
    public boolean isOpen() {
        return gate.queryState() == ParktronBarrierGate.STATE_RAISED;
    }
}
