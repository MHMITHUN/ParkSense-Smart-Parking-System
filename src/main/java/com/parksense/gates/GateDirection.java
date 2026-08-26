package com.parksense.gates;

/** Which side of the fence a lane sits on. */
public enum GateDirection {
    ENTRY("Entry"),
    EXIT("Exit");

    private final String label;

    GateDirection(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
