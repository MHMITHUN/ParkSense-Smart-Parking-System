package com.parksense.members;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A monthly-pass holder. A member registers one or more plates; any of
 * them enters under the pass while it is valid.
 */
public final class Member {

    private final String id;
    private final String name;
    private final String phone;
    private final Set<String> plates = new LinkedHashSet<>();
    private final String planName;
    private final LocalDate validUntil;
    private volatile boolean active = true;

    public Member(String id, String name, String phone, Set<String> plates,
                  String planName, LocalDate validUntil) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        plates.forEach(p -> this.plates.add(p.trim().toUpperCase()));
        this.planName = planName;
        this.validUntil = validUntil;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String phone() {
        return phone;
    }

    public Set<String> plates() {
        return Set.copyOf(plates);
    }

    public String planName() {
        return planName;
    }

    public LocalDate validUntil() {
        return validUntil;
    }

    public boolean active() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean coversPlate(String plateNo) {
        return plates.contains(plateNo == null ? "" : plateNo.trim().toUpperCase());
    }

    public boolean passValidOn(LocalDate date) {
        return active && !date.isAfter(validUntil);
    }
}
