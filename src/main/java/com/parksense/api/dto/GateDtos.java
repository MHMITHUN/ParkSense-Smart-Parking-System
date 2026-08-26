package com.parksense.api.dto;

import java.math.BigDecimal;

/**
 * Gate and ticket request/response shapes.
 */
public final class GateDtos {

    private GateDtos() {
    }

    public record EntryRequest(String plate, String vehicleType, boolean accessible) {
    }

    public record ExitRequestDto(String reference, String method, BigDecimal tendered,
                                 boolean lostTicket) {
    }

    public record UndoResponse(String gateId, String commandId, boolean undone) {
    }
}
