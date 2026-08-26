package com.parksense.entrycheck;

import com.parksense.lot.SlotState;
import com.parksense.lot.SlotType;
import com.parksense.occupancy.OccupancyLedger;
import com.parksense.vehicles.VehicleType;

/** Confirms a free slot exists that can legally serve this vehicle class. */
public final class SlotAvailabilityHandler extends EntryRuleHandler {

    public static final String REASON = "NO SLOT FOR THIS VEHICLE TYPE";

    private final OccupancyLedger ledger;

    public SlotAvailabilityHandler(OccupancyLedger ledger) {
        this.ledger = ledger;
    }

    @Override
    public String ruleName() {
        return "Slot availability";
    }

    @Override
    protected boolean check(EntryContext context) {
        VehicleType type = context.vehicle().type();
        boolean any = ledger.hasSlotFor(type, context.vehicle().accessibleBadge());
        if (!any) {
            context.failed(ruleName(), REASON);
            return false;
        }
        if (type == VehicleType.EV
                && ledger.lot().freeSlots(SlotType.EV_CHARGE) == 0) {
            context.markEvFallbackToStandard();
            context.passed(ruleName(), "no charger free — standard bay will be assigned");
        } else {
            context.passed(ruleName(), "suitable slot free");
        }
        return true;
    }
}
