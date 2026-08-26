package com.parksense.gates.command;

import com.parksense.audit.AuditTrail;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Per-gate executor of {@link GateCommand}s (GoF Command — the Invoker).
 * Submitting runs the command immediately, records an audit line and keeps
 * it in history; the UI can then undo a command by id. The queue is the
 * single funnel through which lane hardware is ever touched.
 */
public final class GateCommandQueue {

    private final String gateCode;
    private final AuditTrail audit;
    private final List<GateCommand> history = new CopyOnWriteArrayList<>();

    public GateCommandQueue(String gateCode, AuditTrail audit) {
        this.gateCode = gateCode;
        this.audit = audit;
    }

    /** Execute now, audit, keep for undo/history. */
    public synchronized GateCommand submit(GateCommand command) {
        command.execute();
        history.add(command);
        audit.record("system", "GATE_COMMAND",
                command.describe() + (command.undoSupported() ? "" : " (no undo)"), true);
        return command;
    }

    /** Reverse an executed command by id. */
    public synchronized Optional<Boolean> undo(String commandId) {
        for (GateCommand command : history) {
            if (command.id().equals(commandId)) {
                boolean undone = command.undo();
                audit.record("system", "GATE_COMMAND_UNDO", command.describe(), undone);
                return Optional.of(undone);
            }
        }
        return Optional.empty();
    }

    /** Newest-first history for the simulator panel. */
    public List<GateCommand> history() {
        List<GateCommand> copy = new ArrayList<>(history);
        java.util.Collections.reverse(copy);
        return copy;
    }

    public String gateCode() {
        return gateCode;
    }
}
