package com.parksense.api;

import com.parksense.app.OperationsFacade;
import com.parksense.app.SystemResetService;
import com.parksense.auth.RequestRoles;
import com.parksense.dev.PatternCatalog;
import com.parksense.occupancy.board.DisplayBoard;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * System surface: board snapshots, live event feed, pattern catalogue,
 * audit trail, sim-clock, reset and health.
 */
@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final OperationsFacade ops;
    private final SystemResetService resetService;
    private final List<DisplayBoard> boards;
    private final com.parksense.audit.AuditTrail audit;

    public SystemController(OperationsFacade ops, SystemResetService resetService,
                            List<DisplayBoard> boards, com.parksense.audit.AuditTrail audit) {
        this.ops = ops;
        this.resetService = resetService;
        this.boards = boards;
        this.audit = audit;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "app", "ParkSense");
    }

    @GetMapping("/boards")
    public List<Map<String, Object>> boards() {
        return boards.stream().map(b -> Map.of(
                "boardId", b.boardId(),
                "title", b.title(),
                "lines", b.render().lines(),
                "updatedAt", b.render().updatedAt().toString()))
                .toList();
    }

    @GetMapping("/events")
    public List<Map<String, Object>> events(@RequestParam(defaultValue = "25") int limit) {
        return ops.recentEvents(limit);
    }

    @GetMapping("/alerts")
    public List<Map<String, Object>> alerts(@RequestParam(defaultValue = "10") int limit) {
        return ops.capacityAlerts(limit);
    }

    @GetMapping("/patterns")
    public List<Map<String, Object>> patterns() {
        return PatternCatalog.catalog();
    }

    @GetMapping("/audit")
    public ResponseEntity<Object> audit(@RequestParam(defaultValue = "50") int limit) {
        if (!RequestRoles.isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "ADMIN only"));
        }
        return ResponseEntity.ok(Map.of("entries", audit.latest(limit)));
    }

    @PostMapping("/clock")
    public ResponseEntity<Object> advanceClock(@RequestBody Map<String, Object> body) {
        if (!RequestRoles.isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "ADMIN only"));
        }
        long minutes = Long.parseLong(String.valueOf(body.getOrDefault("minutes", 0)));
        ops.advanceClock(minutes);
        return ResponseEntity.ok(Map.of("ok", true, "advancedMinutes", minutes));
    }

    @PostMapping("/reset")
    public ResponseEntity<Object> reset() {
        if (!RequestRoles.isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "ADMIN only"));
        }
        resetService.reset();
        return ResponseEntity.ok(Map.of("ok", true, "message", "Runtime reseeded"));
    }
}
