package com.parksense.gates;

import com.parksense.controlroom.Colleague;
import com.parksense.controlroom.ParkingMediator;
import com.parksense.hardware.adapter.ParktronSensorAdapter;
import com.parksense.hardware.spi.SlotSignal;
import com.parksense.lot.Slot;

/**
 * The sensor colleague: polls Parktron nodes through the adapter and
 * forwards readings to the mediator, which decides what they mean (arrival
 * confirmation, unplanned occupancy, departure).
 */
public final class SensorFeed implements Colleague {

    private final String id = "SENSOR-FEED";
    private final ParktronSensorAdapter adapter;
    private ParkingMediator mediator;

    public SensorFeed(ParktronSensorAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public String colleagueId() {
        return id;
    }

    @Override
    public void setMediator(ParkingMediator mediator) {
        this.mediator = mediator;
    }

    /** Simulate the sensor of one slot firing (demo driver + tests). */
    public SlotSignal pollSlot(Slot slot) {
        SlotSignal signal = adapter.poll(slot.code());
        if (mediator != null) {
            mediator.handleSensorSignal(signal);
        }
        return signal;
    }

    /** Drive the simulated hardware: set node presence, then report. */
    public void driveAndReport(Slot slot, boolean vehiclePresent) {
        adapter.reportPresence(slot.code(), vehiclePresent);
        pollSlot(slot);
    }
}
