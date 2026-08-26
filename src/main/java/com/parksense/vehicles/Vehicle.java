package com.parksense.vehicles;

import java.util.Objects;

/**
 * A vehicle observed at a gate. Vehicles are immutable value objects; the
 * system identifies them by plate number and never stores more identity
 * data than a parking operation needs.
 */
public final class Vehicle {

    private final String plateNo;
    private final VehicleType type;
    private final boolean accessibleBadge;

    public Vehicle(String plateNo, VehicleType type, boolean accessibleBadge) {
        this.plateNo = Objects.requireNonNull(plateNo, "plateNo").trim().toUpperCase();
        if (this.plateNo.isEmpty()) {
            throw new IllegalArgumentException("Plate number cannot be empty");
        }
        this.type = Objects.requireNonNull(type, "type");
        this.accessibleBadge = accessibleBadge;
    }

    public String plateNo() {
        return plateNo;
    }

    public VehicleType type() {
        return type;
    }

    /** True when the driver presented a disability-accessibility badge. */
    public boolean accessibleBadge() {
        return accessibleBadge;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Vehicle other && plateNo.equals(other.plateNo);
    }

    @Override
    public int hashCode() {
        return plateNo.hashCode();
    }

    @Override
    public String toString() {
        return plateNo + " (" + type.label() + ")";
    }
}
