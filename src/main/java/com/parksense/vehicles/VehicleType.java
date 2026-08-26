package com.parksense.vehicles;

/**
 * Vehicle classes recognised by the ANPR classifier. Each class drives slot
 * suitability (see {@code SlotType#canServe}) and tariff multipliers.
 */
public enum VehicleType {
    CAR("Car"),
    SUV("SUV"),
    VAN("Van"),
    MOTORCYCLE("Motorcycle"),
    EV("Electric Vehicle");

    private final String label;

    VehicleType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Tariff multiplier relative to a standard car (vans cost more, motorcycles less). */
    public java.math.BigDecimal tariffFactor() {
        return switch (this) {
            case MOTORCYCLE -> new java.math.BigDecimal("0.50");
            case VAN -> new java.math.BigDecimal("1.50");
            case SUV -> new java.math.BigDecimal("1.20");
            case CAR, EV -> java.math.BigDecimal.ONE;
        };
    }
}
