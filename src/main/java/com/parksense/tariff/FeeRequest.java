package com.parksense.tariff;

import com.parksense.vehicles.VehicleType;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Everything a strategy needs to price one exit: the stay window and the
 * vehicle class. Immutable by construction.
 */
public record FeeRequest(Instant entryAt, Instant exitAt, VehicleType vehicleType, ZoneId zone) {

    public Duration stay() {
        return Duration.between(entryAt, exitAt);
    }

    public static FeeRequest of(Instant entryAt, Instant exitAt, VehicleType type) {
        return new FeeRequest(entryAt, exitAt, type, ZoneId.systemDefault());
    }
}
