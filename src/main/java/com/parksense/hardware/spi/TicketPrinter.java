package com.parksense.hardware.spi;

/**
 * Internal port for the entry-lane ticket printer.
 */
public interface TicketPrinter {

    /** Render the entry ticket text; returns the printed content. */
    String print(String ticketNo, String plate, String slotCode, String issuedLine);
}
