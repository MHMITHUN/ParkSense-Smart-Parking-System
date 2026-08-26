package com.parksense.api;

import com.parksense.app.OperationsFacade;
import com.parksense.auth.RequestRoles;
import com.parksense.tariff.TariffKind;
import com.parksense.tariff.TariffPlan;
import com.parksense.tariff.builder.TariffPlanBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tariff administration: builder-backed creation (invalid plans are
 * rejected with the builder's invariant message) and activation toggles.
 */
@RestController
@RequestMapping("/api/tariffs")
public class TariffController {

    private final OperationsFacade ops;

    public TariffController(OperationsFacade ops) {
        this.ops = ops;
    }

    @GetMapping
    public List<Map<String, Object>> tariffs() {
        return ops.tariffs().stream().map(TariffController::toRow).toList();
    }

    @PostMapping
    public ResponseEntity<Object> create(@RequestBody Map<String, Object> body) {
        if (!RequestRoles.isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Only ADMIN may edit tariffs"));
        }
        TariffPlanBuilder builder = new TariffPlanBuilder(
                String.valueOf(body.getOrDefault("id", "TAR-CUSTOM")),
                String.valueOf(body.getOrDefault("name", "Custom plan")),
                TariffKind.valueOf(String.valueOf(body.get("kind")).toUpperCase()));
        if (body.containsKey("baseFee")) {
            builder.baseFee(new BigDecimal(String.valueOf(body.get("baseFee"))));
        }
        if (body.containsKey("perHour")) {
            builder.perHour(new BigDecimal(String.valueOf(body.get("perHour"))));
        }
        if (body.containsKey("dailyCap")) {
            builder.dailyCap(new BigDecimal(String.valueOf(body.get("dailyCap"))));
        }
        if (body.containsKey("graceMinutes")) {
            builder.graceMinutes(Integer.parseInt(String.valueOf(body.get("graceMinutes"))));
        }
        if (body.containsKey("flatFee")) {
            builder.flatFee(new BigDecimal(String.valueOf(body.get("flatFee"))));
        }
        if (body.containsKey("earlyIn") && body.containsKey("earlyOut")) {
            builder.earlyBirdWindow(LocalTime.parse(String.valueOf(body.get("earlyIn"))),
                    LocalTime.parse(String.valueOf(body.get("earlyOut"))));
        }
        if (body.containsKey("surgeMultiplier")) {
            builder.surgeMultiplier(new BigDecimal(String.valueOf(body.get("surgeMultiplier"))));
        }
        builder.active(Boolean.parseBoolean(String.valueOf(body.getOrDefault("active", "false"))));
        try {
            return ResponseEntity.ok(toRow(ops.saveTariff(builder)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Object> activate(@PathVariable String id) {
        if (!RequestRoles.isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Only ADMIN may edit tariffs"));
        }
        ops.setTariffActive(id, true);
        return ResponseEntity.ok(Map.of("id", id, "active", true));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Object> deactivate(@PathVariable String id) {
        if (!RequestRoles.isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Only ADMIN may edit tariffs"));
        }
        ops.setTariffActive(id, false);
        return ResponseEntity.ok(Map.of("id", id, "active", false));
    }

    private static Map<String, Object> toRow(TariffPlan p) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", p.id());
        row.put("name", p.name());
        row.put("kind", p.kind().name());
        row.put("kindLabel", p.kind().label());
        row.put("baseFee", p.baseFee().toPlainString());
        row.put("perHour", p.perHourFee().toPlainString());
        row.put("dailyCap", p.dailyCap() == null ? null : p.dailyCap().toPlainString());
        row.put("graceMinutes", p.graceMinutes());
        row.put("flatFee", p.flatFee() == null ? null : p.flatFee().toPlainString());
        row.put("surgeMultiplier", p.surgeMultiplier().toPlainString());
        row.put("active", p.active());
        return row;
    }
}
