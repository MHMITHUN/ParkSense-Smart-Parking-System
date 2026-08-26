package com.parksense.gates;

import com.parksense.audit.AuditTrail;
import com.parksense.gates.command.CloseBarrierCommand;
import com.parksense.gates.command.GateCommandQueue;
import com.parksense.gates.command.OpenBarrierCommand;
import com.parksense.gates.command.PrintTicketCommand;
import com.parksense.gates.hardware.SimulatedHardwareFactory;
import com.parksense.tickets.Ticket;
import com.parksense.vehicles.VehicleType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** GoF Command — queue executes, audits and undoes lane hardware ops. */
class GateCommandTest {

    private final AuditTrail audit = new AuditTrail();
    private final EntryGate gate = new EntryGate("G-IN", "G-IN",
            new SimulatedHardwareFactory(), audit);
    private final GateCommandQueue queue = gate.commandQueue();

    @Test
    void openCloseCycleLeavesBarrierDown() {
        queue.submit(new OpenBarrierCommand(gate));
        queue.submit(new CloseBarrierCommand(gate));
        assertEquals(2, queue.history().size());
        assertFalse(gate.barrier().isOpen());
        assertTrue(gate.commandQueue().history().get(1).executed());
    }

    @Test
    void undoReversesOpenBarrier() {
        var open = queue.submit(new OpenBarrierCommand(gate));
        assertTrue(gate.barrier().isOpen());
        assertTrue(queue.undo(open.id()).orElse(false));
        assertFalse(gate.barrier().isOpen());
        assertTrue(open.undone());
    }

    @Test
    void printTicketIsNotUndoable() {
        Ticket ticket = new Ticket("T-1", "P-1", VehicleType.CAR, false,
                java.time.Instant.now(), "G-IN", "L1-A-01");
        var print = new PrintTicketCommand(gate, ticket);
        queue.submit(print);
        assertFalse(print.undoSupported());
        assertFalse(queue.undo(print.id()).orElse(true));
        assertTrue(print.printedText().contains("T-1"));
    }

    @Test
    void everySubmissionIsAudited() {
        int before = audit.size();
        queue.submit(new OpenBarrierCommand(gate));
        assertEquals(before + 1, audit.size());
    }

    @Test
    void undoUnknownCommandIdReturnsEmpty() {
        assertTrue(queue.undo("nope").isEmpty());
    }
}
