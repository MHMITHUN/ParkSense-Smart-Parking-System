package com.parksense.gates;

import com.parksense.audit.AuditTrail;
import com.parksense.controlroom.Colleague;
import com.parksense.controlroom.ParkingMediator;
import com.parksense.gates.command.GateCommandQueue;
import com.parksense.gates.hardware.GateHardwareFactory;
import com.parksense.hardware.spi.Barrier;
import com.parksense.hardware.spi.PlateReader;
import com.parksense.hardware.spi.TicketPrinter;

/**
 * One lane of the facility (a Mediator colleague). The gate owns its
 * equipment stack — built by its {@code GateHardwareFactory} — its command
 * queue and its display line, and delegates all coordination decisions to
 * the mediator.
 */
public abstract class Gate implements Colleague {

    private final String id;
    private final String code;
    private final GateDirection direction;
    private final LaneKind laneKind;
    private final PlateReader plateReader;
    private final Barrier barrier;
    private final TicketPrinter printer;
    private final GateCommandQueue commandQueue;
    private volatile ParkingMediator mediator;
    private volatile String displayLine = "READY";

    protected Gate(String id, String code, GateDirection direction, LaneKind laneKind,
                   GateHardwareFactory hardwareFactory, AuditTrail audit) {
        this.id = id;
        this.code = code;
        this.direction = direction;
        this.laneKind = laneKind;
        this.plateReader = hardwareFactory.createPlateReader(code);
        this.barrier = hardwareFactory.createBarrier(code);
        this.printer = hardwareFactory.createTicketPrinter();
        this.commandQueue = new GateCommandQueue(code, audit);
    }

    @Override
    public String colleagueId() {
        return id;
    }

    @Override
    public void setMediator(ParkingMediator mediator) {
        this.mediator = mediator;
    }

    protected ParkingMediator mediator() {
        if (mediator == null) {
            throw new IllegalStateException("Gate " + code + " has no mediator attached");
        }
        return mediator;
    }

    public String id() {
        return id;
    }

    public String code() {
        return code;
    }

    public GateDirection direction() {
        return direction;
    }

    public LaneKind laneKind() {
        return laneKind;
    }

    public PlateReader plateReader() {
        return plateReader;
    }

    public Barrier barrier() {
        return barrier;
    }

    public TicketPrinter printer() {
        return printer;
    }

    public GateCommandQueue commandQueue() {
        return commandQueue;
    }

    public String displayLine() {
        return displayLine;
    }

    public void setDisplay(String line) {
        this.displayLine = line;
    }

    /** The camera read a plate (normalised through the reader port). */
    public com.parksense.hardware.spi.PlateScan readPlate(String cameraPayload) {
        return plateReader.read(cameraPayload);
    }
}
