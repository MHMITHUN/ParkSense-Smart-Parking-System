package com.parksense.entrycheck;

import com.parksense.lot.SlotType;
import com.parksense.occupancy.OccupancyLedger;
import com.parksense.vehicles.VehicleType;

/**
 * Non-blocking EV check: if every charging bay is gone, the driver is told
 * now (on the trace) that charging will not be possible this stay.
 */
public final class EvChargerHandler extends EntryRuleHandler {

    private final OccupancyLedger ledger;

    public EvChargerHandler(OccupancyLedger ledger) {
        this.ledger = ledger;
    }

    @Override
    public String ruleName() {
        return "EV charger";
    }

    @Override
    protected boolean check(EntryContext context) {
        if (context.vehicle().type() == VehicleType.EV) {
            int free = ledger.lot().freeSlots(SlotType.EV_CHARGE);
            if (free == 0) {
                context.note("EV: all chargers busy — parking only, no top-up");
            } else {
                context.note("EV: " + free + " charger bay(s) free");
            }
        }
        return true;
    }
}
