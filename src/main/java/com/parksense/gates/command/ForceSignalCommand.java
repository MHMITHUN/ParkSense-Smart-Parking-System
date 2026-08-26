package com.parksense.gates.command;

import com.parksense.gates.Gate;

/**
 * Override the lane display with a custom message (GoF Command — undo
 * restores the previous line).
 */
public final class ForceSignalCommand extends GateCommand {

    private final Gate gate;
    private final String message;
    private volatile String previousLine;

    public ForceSignalCommand(Gate gate, String message) {
        this.gate = gate;
        this.message = message;
    }

    @Override
    protected void onExecute() {
        previousLine = gate.displayLine();
        gate.setDisplay(message);
    }

    @Override
    protected void onUndo() {
        gate.setDisplay(previousLine == null ? "READY" : previousLine);
    }

    @Override
    public String describe() {
        return "Signal \"" + message + "\" on " + gate.code();
    }
}
