package com.parksense.api;

import com.parksense.api.dto.MapDtos.FloorDto;
import com.parksense.api.dto.MapDtos.FloorFreeDto;
import com.parksense.api.dto.MapDtos.MapDto;
import com.parksense.api.dto.MapDtos.MapSummaryDto;
import com.parksense.api.dto.MapDtos.SlotDto;
import com.parksense.api.dto.MapDtos.TypeFreeDto;
import com.parksense.api.dto.MapDtos.ZoneDto;
import com.parksense.lot.Floor;
import com.parksense.lot.ParkingLot;
import com.parksense.lot.Slot;
import com.parksense.lot.SlotType;
import com.parksense.lot.Zone;
import com.parksense.occupancy.OccupancyLedger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read endpoints for the live parking map. The tree walk happens on the
 * composite itself; the controller only projects nodes to DTOs.
 */
@RestController
@RequestMapping("/api/map")
public class MapController {

    private final OccupancyLedger ledger;

    public MapController(OccupancyLedger ledger) {
        this.ledger = ledger;
    }

    @GetMapping
    public MapDto map() {
        ParkingLot lot = ledger.lot();
        return new MapDto(lot.id(), lot.name(), lot.totalSlots(), lot.freeSlots(),
                lot.floors().stream().map(this::toFloor).toList());
    }

    @GetMapping("/summary")
    public MapSummaryDto summary() {
        ParkingLot lot = ledger.lot();
        List<TypeFreeDto> byType = List.of(
                new TypeFreeDto(SlotType.STANDARD.name(), lot.freeSlots(SlotType.STANDARD)),
                new TypeFreeDto(SlotType.COMPACT.name(), lot.freeSlots(SlotType.COMPACT)),
                new TypeFreeDto(SlotType.ACCESSIBLE.name(), lot.freeSlots(SlotType.ACCESSIBLE)),
                new TypeFreeDto(SlotType.EV_CHARGE.name(), lot.freeSlots(SlotType.EV_CHARGE)),
                new TypeFreeDto(SlotType.MOTORCYCLE.name(), lot.freeSlots(SlotType.MOTORCYCLE)));
        List<FloorFreeDto> byFloor = lot.floors().stream()
                .map(f -> new FloorFreeDto(f.code(), f.freeSlots(), f.totalSlots()))
                .toList();
        return new MapSummaryDto(lot.id(), lot.totalSlots(), lot.freeSlots(), byType, byFloor);
    }

    @GetMapping("/slots")
    public List<SlotDto> slots(@RequestParam(required = false) String floor,
                               @RequestParam(required = false) String type,
                               @RequestParam(required = false) String state) {
        return ledger.lot().slots().stream()
                .filter(s -> floor == null || floor.isBlank() || s.code().startsWith(floor + "-"))
                .filter(s -> type == null || type.isBlank() || s.type().name().equalsIgnoreCase(type))
                .filter(s -> state == null || state.isBlank() || s.state().name().equalsIgnoreCase(state))
                .map(MapController::toSlot)
                .toList();
    }

    private FloorDto toFloor(Floor floor) {
        return new FloorDto(floor.code(), floor.label(), floor.totalSlots(), floor.freeSlots(),
                floor.zones().stream().map(this::toZone).toList());
    }

    private ZoneDto toZone(Zone zone) {
        return new ZoneDto(zone.code(), zone.label(), zone.totalSlots(), zone.freeSlots(),
                zone.slots().stream().map(MapController::toSlot).toList());
    }

    private static SlotDto toSlot(Slot slot) {
        return new SlotDto(slot.code(), slot.type().name(), slot.state().name(),
                slot.currentTicketNo(), slot.currentPlate(),
                slot.occupiedSince() == null ? null : slot.occupiedSince().toString());
    }
}
