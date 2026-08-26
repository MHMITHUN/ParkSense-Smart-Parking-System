package com.parksense.hardware;

import com.parksense.hardware.adapter.ParktronBarrierAdapter;
import com.parksense.hardware.adapter.ParktronSensorAdapter;
import com.parksense.hardware.adapter.PlateSenseAdapter;
import com.parksense.hardware.vendor.ParktronBarrierGate;
import com.parksense.hardware.vendor.ParktronSensorNetwork;
import com.parksense.hardware.vendor.PlateSenseAnprCamera;
import com.parksense.gates.hardware.GateHardwareFactory;
import com.parksense.gates.hardware.SimulatedHardwareFactory;
import com.parksense.gates.hardware.VendorHardwareFactory;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** GoF Adapter + Factory Method — vendor SDKs behind internal ports. */
class HardwareAdaptersTest {

    @Test
    void plateSenseAdapterTranslatesVendorScan() {
        PlateSenseAdapter adapter = new PlateSenseAdapter(new PlateSenseAnprCamera("FX200"));
        var scan = adapter.read("DHAKA METRO GA 11-2233|97%");
        assertEquals("DHAKA-METRO-GA-11-2233", scan.plateNo());
        assertEquals(0.97, scan.confidence(), 0.0001);
        assertTrue(scan.cameraId().startsWith("FX200/"));
        assertNotNull(scan.readAt());
    }

    @Test
    void adapterSurvivesGarbagePayload() {
        PlateSenseAdapter adapter = new PlateSenseAdapter(new PlateSenseAnprCamera("FX200"));
        assertEquals("UNREADABLE", adapter.read("").plateNo());
        assertNotNull(adapter.read("|||").readAt());
    }

    @Test
    void sensorAdapterDecodesBitmaskWords() {
        ParktronSensorNetwork network = new ParktronSensorNetwork();
        ParktronSensorAdapter adapter = new ParktronSensorAdapter(network);
        adapter.reportPresence("L1-A-04", true);
        assertTrue(adapter.poll("L1-A-04").vehiclePresent());
        adapter.reportPresence("L1-A-04", false);
        assertFalse(adapter.poll("L1-A-04").vehiclePresent());
    }

    @Test
    void barrierAdapterDrivesTheMotorController() {
        ParktronBarrierAdapter barrier = new ParktronBarrierAdapter(new ParktronBarrierGate());
        assertFalse(barrier.isOpen());
        barrier.raise();
        assertTrue(barrier.isOpen());
        barrier.lower();
        assertFalse(barrier.isOpen());
    }

    @Test
    void simulatedFactoryBuildsWorkingEquipment() {
        GateHardwareFactory factory = new SimulatedHardwareFactory();
        var reader = factory.createPlateReader("L1");
        assertEquals("SIM-1", reader.read("SIM 1").plateNo());
        var barrier = factory.createBarrier("L1");
        barrier.raise();
        assertTrue(barrier.isOpen());
        assertTrue(factory.createTicketPrinter().print("T", "P", "S", "now").contains("PARKSENSE"));
    }

    @Test
    void vendorFactoryBuildsAdaptedEquipment() {
        GateHardwareFactory factory = new VendorHardwareFactory();
        assertTrue(factory.createPlateReader("L1") instanceof PlateSenseAdapter);
        assertTrue(factory.createBarrier("L1") instanceof ParktronBarrierAdapter);
        assertEquals("VENDOR", factory.familyName());
    }
}
