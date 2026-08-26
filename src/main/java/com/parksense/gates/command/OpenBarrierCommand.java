package com.parksense.gates.command;

import com.parksense.gates.Gate;

/**
 * Raise the barrier arm and greet the driver (GoF Command — reversible:
 * undo lowers it again).
 */
public final class OpenBarrierCommand extends GateCommand {

    private final Gate gate;

    public OpenBarrierCommand(Gate gate) {
        this.gate = gate;
    }

    @Override
    protected void onExecute() {
        gate.barrier().raise();
        gate.setDisplay("BARRIER OPEN — PLEASE PROCEED");
    }

    @Override
    protected void onUndo() {
        gate.barrier().lower();
        gate.setDisplay("BARRIER RESET");
    }

    @Override
    public String describe() {
        return "Open barrier " + gate.code();
    }
}
