package com.parksense.entrycheck;

import com.parksense.members.MemberRegistry;
import com.parksense.occupancy.OccupancyLedger;
import com.parksense.vehicles.PlateRegistry;

/**
 * Assembles the entry chain in its one true order (GoF Chain of
 * Responsibility — the wiring lives in a single place, so the order is
 * policy, not accident):
 *
 * blacklist → duplicate entry → member recognition → slot availability →
 * EV charger note → capacity note.
 */
public final class EntryChainFactory {

    private EntryChainFactory() {
    }

    public static EntryRuleHandler build(PlateRegistry plates, MemberRegistry members,
                                         OccupancyLedger ledger) {
        BlacklistHandler blacklist = new BlacklistHandler(plates);
        blacklist.linkWith(new DuplicateEntryHandler(plates))
                .linkWith(new MemberRecognitionHandler(members))
                .linkWith(new SlotAvailabilityHandler(ledger))
                .linkWith(new EvChargerHandler(ledger))
                .linkWith(new CapacityAnnouncementHandler(ledger));
        return blacklist;
    }
}
