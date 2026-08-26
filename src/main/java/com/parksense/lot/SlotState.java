package com.parksense.lot;

/**
 * Lifecycle of a single parking slot. Transitions are guarded exclusively by
 * the {@code OccupancyLedger} so exactly one component owns the rule
 * "a slot changes state only through a legal step".
 *
 * <pre>
 * OUT_OF_SERVICE &lt;-&gt; FREE -&gt; RESERVED -&gt; OCCUPIED -&gt; FREE
 *                        ^                    |
 *                        +-- no-show timeout -+
 * </pre>
 */
public enum SlotState {
    FREE("Free"),
    RESERVED("Reserved"),
    OCCUPIED("Occupied"),
    OUT_OF_SERVICE("Out of service");

    private final String label;

    SlotState(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Legal successor states from this state (for documentation and tests). */
    public SlotState[] legalNext() {
        return switch (this) {
            case FREE -> new SlotState[]{SlotState.RESERVED, SlotState.OUT_OF_SERVICE};
            case RESERVED -> new SlotState[]{SlotState.OCCUPIED, SlotState.FREE, SlotState.OUT_OF_SERVICE};
            case OCCUPIED -> new SlotState[]{SlotState.FREE, SlotState.OUT_OF_SERVICE};
            case OUT_OF_SERVICE -> new SlotState[]{SlotState.FREE};
        };
    }
}
