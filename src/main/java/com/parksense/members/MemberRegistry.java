package com.parksense.members;

import com.parksense.store.MemberStore;
import com.parksense.time.SimClock;

import java.util.Optional;

/**
 * Answers the one question the exit lane asks: "does this plate have a
 * valid pass right now?" Wraps the store plus the validity rule (expiry
 * checked against the simulated clock, so demos can time-travel).
 */
public final class MemberRegistry {

    private final MemberStore store;
    private final SimClock clock;

    public MemberRegistry(MemberStore store, SimClock clock) {
        this.store = store;
        this.clock = clock;
    }

    public Optional<Member> byPlate(String plateNo) {
        return store.findByPlate(plateNo);
    }

    public boolean hasValidPass(String plateNo) {
        return store.findByPlate(plateNo)
                .map(m -> m.passValidOn(clock.today()))
                .orElse(false);
    }
}
