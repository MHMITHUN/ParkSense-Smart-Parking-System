package com.parksense.tickets.state;

import com.parksense.tickets.PaymentRecord;
import com.parksense.tickets.Ticket;

/**
 * Behaviour of a ticket in one lifecycle state (GoF State). Each state is
 * a stateless singleton; the ticket owns a reference to its current state
 * and every operation is first offered to that state. States that do not
 * support an operation reject it with {@link IllegalTransitionException}.
 *
 * <pre>
 * ISSUED → ACTIVE → PAID → EXITED
 *           │  ▲      │
 *           │  └──────┘ overstay reopen
 *           ├→ LOST ──pay penalty──→ EXITED
 *           └→ VOID (also from PAID, ADMIN only)
 * </pre>
 */
public interface TicketState {

    String name();

    /** Barrier opened after issue — the stay has begun. */
    default void confirmEntry(Ticket ticket) {
        throw new IllegalTransitionException("Cannot confirm entry from state " + name());
    }

    /** Register a payment. */
    default void pay(Ticket ticket, PaymentRecord payment) {
        throw new IllegalTransitionException("Cannot pay a ticket in state " + name());
    }

    /** Vehicle passed the exit barrier. */
    default void markExited(Ticket ticket) {
        throw new IllegalTransitionException("Cannot exit a ticket in state " + name());
    }

    /** Driver reports the ticket lost. */
    default void reportLost(Ticket ticket) {
        throw new IllegalTransitionException("Cannot report loss from state " + name());
    }

    /** ADMIN cancels a ticket entirely. */
    default void voidTicket(Ticket ticket, String reason) {
        throw new IllegalTransitionException("Cannot void a ticket in state " + name());
    }

    /** Paid driver overstayed the exit grace — reopen the fee. */
    default void reopenFromOverstay(Ticket ticket) {
        throw new IllegalTransitionException("Cannot reopen a ticket in state " + name());
    }
}
