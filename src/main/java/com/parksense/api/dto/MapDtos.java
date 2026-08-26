package com.parksense.api.dto;

import java.util.List;

/**
 * Read-only shapes of the lot tree for the live map and summary polls.
 */
public final class MapDtos {

    private MapDtos() {
    }

    public record SlotDto(String code, String type, String state,
                          String ticketNo, String plate, String occupiedSince) {
    }

    public record ZoneDto(String code, String label, int totalSlots, int freeSlots,
                          List<SlotDto> slots) {
    }

    public record FloorDto(String code, String label, int totalSlots, int freeSlots,
                           List<ZoneDto> zones) {
    }

    public record MapDto(String lotId, String lotName, int totalSlots, int freeSlots,
                         List<FloorDto> floors) {
    }

    public record TypeFreeDto(String type, int free) {
    }

    public record FloorFreeDto(String floor, int free, int total) {
    }

    public record MapSummaryDto(String lotId, int totalSlots, int freeSlots,
                                List<TypeFreeDto> freeByType, List<FloorFreeDto> freeByFloor) {
    }
}
