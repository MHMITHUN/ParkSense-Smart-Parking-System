package com.parksense.tickets;

/**
 * Visitor over closed tickets and their fee chains (GoF Visitor, element
 * side declared with the elements it visits). Reports implement this and
 * are accepted per ticket — the ticket never knows which report is asking.
 */
public interface TicketVisitor {

    default void visitTicket(Ticket ticket) {
    }

    default void visitFeeLine(FeeComponent.FeeLine line) {
    }
}
