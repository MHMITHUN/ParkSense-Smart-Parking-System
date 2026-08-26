package com.parksense.app;

import com.parksense.occupancy.OccupancyLedger;
import com.parksense.persistence.MongoSync;
import com.parksense.store.TicketStore;
import com.parksense.tariff.TariffSelector;
import com.parksense.tickets.TicketNoGenerator;
import com.parksense.time.SimClock;
import com.parksense.vehicles.PlateRegistry;

/**
 * Wipes runtime state and replays the deterministic seed — the demo's
 * "clean slate" button. Slot states reset through the ledger's rules, so
 * even a reset cannot corrupt the occupancy truth; when MongoDB
 * persistence is active the collections are dropped and re-snapshotted
 * too. (Plain class; wired as a bean in AppConfig.)
 */
public class SystemResetService {

    private final OccupancyLedger ledger;
    private final TicketStore tickets;
    private final PlateRegistry plates;
    private final TicketNoGenerator ticketNos;
    private final SimClock clock;
    private final TariffSelector selector;
    private final MongoSync persistence;

    public SystemResetService(OccupancyLedger ledger, TicketStore tickets,
                              PlateRegistry plates, TicketNoGenerator ticketNos,
                              SimClock clock, TariffSelector selector,
                              MongoSync persistence) {
        this.ledger = ledger;
        this.tickets = tickets;
        this.plates = plates;
        this.ticketNos = ticketNos;
        this.clock = clock;
        this.selector = selector;
        this.persistence = persistence;
    }

    public synchronized void reset() {
        persistence.dropAll();
        ledger.lot().slots().forEach(slot -> {
            switch (slot.state()) {
                case OCCUPIED, RESERVED -> {
                    slot.vacate();
                    ledger.release(slot, "system reset");
                }
                case OUT_OF_SERVICE -> ledger.returnToService(slot.code());
                case FREE -> {
                }
            }
        });
        tickets.clear();
        plates.clear();
        clock.resetToSystem();
        SeedData.seedRuntime(ledger, tickets, plates, ticketNos, clock, selector);
        persistence.snapshotAll();
    }
}
