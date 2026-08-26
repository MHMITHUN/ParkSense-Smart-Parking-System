package com.parksense.gates;

/** How a lane is operated. */
public enum LaneKind {
    STAFFED("Staffed booth"),
    EXPRESS("Express / member");

    private final String label;

    LaneKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
