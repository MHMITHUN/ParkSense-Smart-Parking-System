package com.parksense.gates;

/**
 * The real subject behind {@link GateControl}: drives the lane's physical
 * barrier directly, bypassing the command queue (an emergency cannot wait
 * behind queued prints).
 */
public final class BarrierController implements GateControl {

    private final Gate gate;
    private volatile boolean manualHold;

    public BarrierController(Gate gate) {
        this.gate = gate;
    }

    @Override
    public void forceOpen(String actor) {
        manualHold = true;
        gate.barrier().raise();
        gate.setDisplay("MANUAL HOLD — OPEN (" + actor + ")");
    }

    @Override
    public void forceClose(String actor) {
        manualHold = true;
        gate.barrier().lower();
        gate.setDisplay("MANUAL HOLD — CLOSED (" + actor + ")");
    }

    @Override
    public void resumeAutomatic(String actor) {
        manualHold = false;
        gate.setDisplay("READY");
    }

    public boolean manualHold() {
        return manualHold;
    }
}
