package com.parksense.hardware.spi;

/**
 * Internal port for licence-plate capture. Gates depend on this interface;
 * whether a simulated camera or a vendor adapter sits behind it is decided
 * by the hardware factory at boot.
 */
public interface PlateReader {

    /**
     * Read one plate. {@code rawCameraPayload} is the incoming camera frame
     * (in simulation: the text the operator's "camera" produced).
     */
    PlateScan read(String rawCameraPayload);
}
