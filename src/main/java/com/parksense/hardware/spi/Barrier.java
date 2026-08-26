package com.parksense.hardware.spi;

/**
 * Internal port for a lane barrier arm.
 */
public interface Barrier {

    void raise();

    void lower();

    boolean isOpen();
}
