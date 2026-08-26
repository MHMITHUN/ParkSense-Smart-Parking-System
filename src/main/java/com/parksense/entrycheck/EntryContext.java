package com.parksense.entrycheck;

import com.parksense.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.List;

/**
 * Working object passed down the entry chain. Handlers annotate it with
 * findings (member plate, EV fallback) and the first rejecting handler
 * stops the whole chain with a driver-readable reason.
 */
public final class EntryContext {

    private final String gateId;
    private final Vehicle vehicle;
    private final List<String> trace = new ArrayList<>();

    private boolean memberPlate;
    private boolean evFallbackToStandard;

    public EntryContext(String gateId, Vehicle vehicle) {
        this.gateId = gateId;
        this.vehicle = vehicle;
    }

    public String gateId() {
        return gateId;
    }

    public Vehicle vehicle() {
        return vehicle;
    }

    /** One check that passed — shown step-by-step in the gate simulator. */
    public void passed(String rule, String detail) {
        trace.add("PASS " + rule + (detail == null || detail.isBlank() ? "" : ": " + detail));
    }

    /** The check that failed, ending the chain. */
    public void failed(String rule, String reason) {
        trace.add("FAIL " + rule + ": " + reason);
    }

    /** Non-decisive observation worth showing in the trace. */
    public void note(String text) {
        trace.add("NOTE " + text);
    }

    public List<String> trace() {
        return List.copyOf(trace);
    }

    public boolean memberPlate() {
        return memberPlate;
    }

    public void markMemberPlate() {
        this.memberPlate = true;
    }

    public boolean evFallbackToStandard() {
        return evFallbackToStandard;
    }

    public void markEvFallbackToStandard() {
        this.evFallbackToStandard = true;
    }
}
