package com.parksense.tariff;

/**
 * The five pricing schemes ParkSense ships with. One {@code TariffStrategy}
 * implementation exists per kind; the {@code TariffSelector} decides which
 * one applies to a given exit.
 */
public enum TariffKind {
    HOURLY("Hourly"),
    DAILY_CAP("Hourly with daily cap"),
    EARLY_BIRD("Early-bird flat"),
    EVENT_SURGE("Event surge"),
    MEMBER_PASS("Member pass");

    private final String label;

    TariffKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
