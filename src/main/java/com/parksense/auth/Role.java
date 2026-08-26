package com.parksense.auth;

/**
 * The two staff roles of ParkSense. ADMIN owns configuration and emergency
 * gate control; OPERATOR runs day-to-day entry, exit and payment lanes.
 */
public enum Role {
    ADMIN("Administrator"),
    OPERATOR("Operator"),
    DEVELOPER("Developer");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
