package com.parksense.gates.command;

import com.parksense.gates.Gate;

/**
 * Lower the barrier arm after the vehicle clears the loop (GoF Command).
 */
public final class CloseBarrierCommand extends GateCommand {

    private final Gate gate;

    public CloseBarrierCommand(Gate gate) {
        this.gate = gate;
    }

    @Override
    protected void onExecute() {
        gate.barrier().lower();
    }

    @Override
    protected void onUndo() {
        gate.barrier().raise();
    }

    @Override
    public String describe() {
        return "Close barrier " + gate.code();
    }
}
