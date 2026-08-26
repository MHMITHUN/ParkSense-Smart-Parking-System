package com.parksense.entrycheck;

import com.parksense.vehicles.PlateRegistry;

/** One open ticket per plate: a second entry is a runaway or a misread. */
public final class DuplicateEntryHandler extends EntryRuleHandler {

    public static final String REASON = "ALREADY INSIDE — EXIT FIRST";

    private final PlateRegistry registry;

    public DuplicateEntryHandler(PlateRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String ruleName() {
        return "Duplicate entry";
    }

    @Override
    protected boolean check(EntryContext context) {
        if (registry.isInside(context.vehicle().plateNo())) {
            context.failed(ruleName(), REASON);
            return false;
        }
        context.passed(ruleName(), "no open stay");
        return true;
    }
}
