package com.parksense.gates.command;

import java.time.Instant;
import java.util.UUID;

/**
 * One queued instruction to lane hardware (GoF Command). Every barrier
 * movement, ticket print and forced display message is a command object —
 * executed through the {@code GateCommandQueue}, logged with an id, and
 * reversible where the hardware allows it.
 */
public abstract class GateCommand {

    private final String id = UUID.randomUUID().toString().substring(0, 8);
    private final Instant queuedAt = Instant.now();
    private volatile Instant executedAt;
    private volatile boolean undone;

    /** Subclasses perform the actual hardware effect here. */
    protected abstract void onExecute();

    /** What this command did, for the queue display and audit log. */
    public abstract String describe();

    /** False for effects that cannot be reversed (a printed ticket). */
    public boolean undoSupported() {
        return true;
    }

    /** Reverse the effect; subclasses override when undoable. */
    protected void onUndo() {
    }

    public final void execute() {
        onExecute();
        executedAt = Instant.now();
    }

    /** @return true when the undo actually happened. */
    public final boolean undo() {
        if (!undoSupported() || executedAt == null || undone) {
            return false;
        }
        onUndo();
        undone = true;
        return true;
    }

    public String id() {
        return id;
    }

    public Instant queuedAt() {
        return queuedAt;
    }

    public Instant executedAt() {
        return executedAt;
    }

    public boolean executed() {
        return executedAt != null;
    }

    public boolean undone() {
        return undone;
    }
}
