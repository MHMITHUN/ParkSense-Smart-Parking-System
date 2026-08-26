package com.parksense.lot;

import com.parksense.vehicles.VehicleType;

/**
 * The physical category of a parking slot. Slot types decide which vehicles a
 * slot may serve; the allocation policy in {@code OccupancyLedger} uses
 * {@link #canServe(VehicleType)} before handing a slot to an arriving vehicle.
 */
public enum SlotType {
    STANDARD("Standard"),
    COMPACT("Compact"),
    ACCESSIBLE("Accessible"),
    EV_CHARGE("EV Charging"),
    MOTORCYCLE("Motorcycle");

    private final String label;

    SlotType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** True when a vehicle of the given type may legally park in this slot category. */
    public boolean canServe(VehicleType vehicle) {
        return switch (this) {
            case MOTORCYCLE -> vehicle == VehicleType.MOTORCYCLE;
            case EV_CHARGE -> vehicle == VehicleType.EV;          // EVs get priority here
            case ACCESSIBLE -> vehicle != VehicleType.MOTORCYCLE; // badge holders of any car class
            case COMPACT -> vehicle == VehicleType.CAR;
            case STANDARD -> vehicle == VehicleType.CAR
                    || vehicle == VehicleType.SUV
                    || vehicle == VehicleType.VAN
                    || vehicle == VehicleType.EV;
        };
    }
}
