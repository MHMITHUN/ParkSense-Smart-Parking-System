package com.parksense.entrycheck;

import com.parksense.occupancy.OccupancyLedger;

/**
 * Terminal handler: annotates how tight the lot is, which the lane can
 * relay to the driver and the dashboard aggregates.
 */
public final class CapacityAnnouncementHandler extends EntryRuleHandler {

    private final OccupancyLedger ledger;

    public CapacityAnnouncementHandler(OccupancyLedger ledger) {
        this.ledger = ledger;
    }

    @Override
    public String ruleName() {
        return "Capacity";
    }

    @Override
    protected boolean check(EntryContext context) {
        int total = ledger.lot().totalSlots();
        int free = ledger.lot().freeSlots();
        int pct = total == 0 ? 0 : free * 100 / total;
        context.note("lot " + pct + "% free (" + free + " bays)");
        return true;
    }
}
