package com.parksense.entrycheck;

import com.parksense.vehicles.PlateRegistry;

/** Rejects stolen vehicles and plates with unpaid fines. */
public final class BlacklistHandler extends EntryRuleHandler {

    public static final String REASON = "VEHICLE BLOCKED — SEE OFFICE";

    private final PlateRegistry registry;

    public BlacklistHandler(PlateRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String ruleName() {
        return "Blacklist";
    }

    @Override
    protected boolean check(EntryContext context) {
        if (registry.isBlacklisted(context.vehicle().plateNo())) {
            context.failed(ruleName(), REASON);
            return false;
        }
        context.passed(ruleName(), "plate clear");
        return true;
    }
}
