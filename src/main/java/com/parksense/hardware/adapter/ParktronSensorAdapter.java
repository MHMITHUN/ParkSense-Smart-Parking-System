package com.parksense.hardware.adapter;

import com.parksense.hardware.spi.OccupancySensor;
import com.parksense.hardware.spi.SlotSignal;
import com.parksense.hardware.vendor.ParktronSensorNetwork;

import java.time.Instant;

/**
 * GoF Adapter: turns Parktron PT-600 packed status words into
 * {@code SlotSignal} readings, mapping the sensor network's node
 * addresses from slot codes ({@code L1-A-04 → PTN-L1A04}).
 */
public final class ParktronSensorAdapter implements OccupancySensor {

    private final ParktronSensorNetwork network;

    public ParktronSensorAdapter(ParktronSensorNetwork network) {
        this.network = network;
    }

    @Override
    public SlotSignal poll(String slotCode) {
        String nodeAddr = nodeAddressOf(slotCode);
        int word = network.pollNode(nodeAddr);
        boolean present = ParktronSensorNetwork.isPresent(word)
                && ParktronSensorNetwork.isOnline(word);
        return new SlotSignal(slotCode, present, Instant.now());
    }

    /** Exposed so the control room can drive the simulated hardware. */
    public void reportPresence(String slotCode, boolean vehiclePresent) {
        String nodeAddr = nodeAddressOf(slotCode);
        int word = ParktronSensorNetwork.BIT_ONLINE;
        if (vehiclePresent) {
            word |= ParktronSensorNetwork.BIT_PRESENT;
        }
        network.overrideNodeWord(nodeAddr, word);
    }

    static String nodeAddressOf(String slotCode) {
        return "PTN-" + slotCode.replace("-", "");
    }
}
