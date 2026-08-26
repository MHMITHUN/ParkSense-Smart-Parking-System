package com.parksense.gates.command;

import com.parksense.gates.Gate;
import com.parksense.tickets.Ticket;

/**
 * Print the entry ticket at the lane printer (GoF Command — paper cannot
 * be unprinted, so undo is not supported).
 */
public final class PrintTicketCommand extends GateCommand {

    private final Gate gate;
    private final Ticket ticket;
    private volatile String printedText;

    public PrintTicketCommand(Gate gate, Ticket ticket) {
        this.gate = gate;
        this.ticket = ticket;
    }

    @Override
    protected void onExecute() {
        printedText = gate.printer().print(
                ticket.ticketNo(), ticket.plate(), ticket.slotCode(),
                ticket.entryTime().toString());
    }

    @Override
    public String describe() {
        return "Print ticket " + ticket.ticketNo();
    }

    @Override
    public boolean undoSupported() {
        return false;
    }

    public String printedText() {
        return printedText;
    }
}
