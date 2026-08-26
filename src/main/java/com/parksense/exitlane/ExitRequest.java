package com.parksense.exitlane;

import com.parksense.tickets.PaymentMethod;

import java.math.BigDecimal;

/**
 * What the exit booth knows when a vehicle presents itself: the ticket
 * number or just the plate, tender details if settling at the booth, and
 * whether the driver reported the ticket lost.
 */
public record ExitRequest(String gateId, String plateOrTicketNo,
                          PaymentMethod method, BigDecimal tendered, boolean lostTicket) {

    public static ExitRequest of(String gateId, String ref) {
        return new ExitRequest(gateId, ref, null, null, false);
    }

    public static ExitRequest lost(String gateId, String plate) {
        return new ExitRequest(gateId, plate, null, null, true);
    }

    public static ExitRequest paying(String gateId, String ref, PaymentMethod method,
                                     BigDecimal tendered) {
        return new ExitRequest(gateId, ref, method, tendered, false);
    }
}
