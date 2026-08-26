package com.parksense.vehicles;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of plate-level facts the entry chain consults: the blacklist
 * (stolen vehicles, unpaid fines) and the set of plates currently inside
 * the lot. Keeping these in one registry means the blacklist handler and
 * the duplicate-ticket handler read the same truth.
 */
public class PlateRegistry {

    private final Set<String> blacklisted = ConcurrentHashMap.newKeySet();
    private final Set<String> insideLot = ConcurrentHashMap.newKeySet();

    public void blacklist(String plateNo) {
        blacklisted.add(normalize(plateNo));
    }

    public void removeFromBlacklist(String plateNo) {
        blacklisted.remove(normalize(plateNo));
    }

    public boolean isBlacklisted(String plateNo) {
        return blacklisted.contains(normalize(plateNo));
    }

    public void markInside(String plateNo) {
        insideLot.add(normalize(plateNo));
    }

    public void markExited(String plateNo) {
        insideLot.remove(normalize(plateNo));
    }

    public boolean isInside(String plateNo) {
        return insideLot.contains(normalize(plateNo));
    }

    public Set<String> blacklistedPlates() {
        return Collections.unmodifiableSet(blacklisted);
    }

    public Set<String> platesInside() {
        return Collections.unmodifiableSet(insideLot);
    }

    /** Forget everything (system reset). */
    public void clear() {
        blacklisted.clear();
        insideLot.clear();
        seedDefaults();
    }

    private void seedDefaults() {
        blacklist("STOLEN-01");
        blacklist("FINES-DUE-7");
    }

    private String normalize(String plateNo) {
        return plateNo == null ? "" : plateNo.trim().toUpperCase();
    }
}
