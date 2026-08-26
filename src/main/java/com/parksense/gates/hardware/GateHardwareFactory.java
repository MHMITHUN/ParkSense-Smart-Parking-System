package com.parksense.gates.hardware;

import com.parksense.hardware.spi.Barrier;
import com.parksense.hardware.spi.PlateReader;
import com.parksense.hardware.spi.TicketPrinter;

/**
 * GoF Factory Method. A lane's whole equipment stack — plate reader,
 * barrier, printer — comes from one factory, and each concrete factory
 * decides which family to build: the in-process simulation kit used by the
 * demo, or vendor-hardware adapters for a real deployment. Gate logic
 * never learns which family it got.
 */
public abstract class GateHardwareFactory {

    /** Which family this factory builds — for logs and the UI. */
    public abstract String familyName();

    /** Factory methods — one per device in the lane equipment stack. */
    public abstract PlateReader createPlateReader(String laneId);

    public abstract Barrier createBarrier(String laneId);

    public TicketPrinter createTicketPrinter() {
        return new com.parksense.hardware.TextReceiptPrinter();
    }
}
