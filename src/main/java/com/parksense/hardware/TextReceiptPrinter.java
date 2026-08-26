package com.parksense.hardware;

import com.parksense.hardware.spi.TicketPrinter;

/**
 * Plain-text entry ticket renderer shared by both hardware families —
 * printing has no vendor variance worth modelling.
 */
public final class TextReceiptPrinter implements TicketPrinter {

    @Override
    public String print(String ticketNo, String plate, String slotCode, String issuedLine) {
        return """
                ----------------------------------------
                  PARKSENSE CENTRAL PLAZA — ENTRY
                ----------------------------------------
                TICKET : %s
                PLATE  : %s
                SLOT   : %s
                IN     : %s
                ----------------------------------------
                Keep this ticket. Present it at exit.
                """.formatted(ticketNo, plate, slotCode, issuedLine);
    }
}
