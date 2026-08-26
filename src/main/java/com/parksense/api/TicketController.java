package com.parksense.api;

import com.parksense.app.CheckoutReceipt;
import com.parksense.app.OperationsFacade;
import com.parksense.auth.RequestRoles;
import com.parksense.tickets.PaymentMethod;
import com.parksense.tickets.Ticket;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ticketing & checkout endpoints: list, detail with fee breakdown, add-on
 * toggles, payment, lost report, void.
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final OperationsFacade ops;

    public TicketController(OperationsFacade ops) {
        this.ops = ops;
    }

    @GetMapping
    public List<Map<String, Object>> tickets(@RequestParam(required = false) String state,
                                             @RequestParam(required = false) String plate,
                                             @RequestParam(defaultValue = "50") int limit) {
        return ops.ticketHistory(500).stream()
                .filter(t -> state == null || state.isBlank()
                        || t.stateName().equalsIgnoreCase(state))
                .filter(t -> plate == null || plate.isBlank()
                        || t.plate().toUpperCase().contains(plate.toUpperCase()))
                .limit(limit)
                .map(TicketController::toRow)
                .toList();
    }

    @GetMapping("/{ticketNo}")
    public Map<String, Object> ticket(@PathVariable String ticketNo) {
        return toDetail(ops.ticket(ticketNo));
    }

    @PostMapping("/{ticketNo}/addons")
    public Map<String, Object> addAddon(@PathVariable String ticketNo,
                                        @RequestBody Map<String, String> body) {
        return Map.of("ticket", ticketNo, "code", body.getOrDefault("code", ""),
                "feeLines", ops.toggleAddon(ticketNo, body.getOrDefault("code", ""), true));
    }

    @DeleteMapping("/{ticketNo}/addons/{code}")
    public Map<String, Object> removeAddon(@PathVariable String ticketNo, @PathVariable String code) {
        return Map.of("ticket", ticketNo, "code", code,
                "feeLines", ops.toggleAddon(ticketNo, code, false));
    }

    @PostMapping("/{ticketNo}/pay")
    public CheckoutReceipt pay(@PathVariable String ticketNo,
                               @RequestBody Map<String, Object> body) {
        PaymentMethod method = body.get("method") == null ? null
                : PaymentMethod.valueOf(String.valueOf(body.get("method")).toUpperCase());
        BigDecimal tendered = body.get("tendered") == null ? null
                : new BigDecimal(String.valueOf(body.get("tendered")));
        return ops.payTicket(ticketNo, method, tendered);
    }

    @PostMapping("/{ticketNo}/lost")
    public Map<String, Object> lost(@PathVariable String ticketNo) {
        ops.reportTicketLost(ticketNo);
        return Map.of("ticket", ticketNo, "state", "LOST");
    }

    @PostMapping("/{ticketNo}/void")
    public ResponseEntity<Object> voidTicket(@PathVariable String ticketNo,
                                             @RequestBody Map<String, String> body) {
        if (!RequestRoles.isAdmin()) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Only ADMIN may void tickets"));
        }
        ops.voidTicket(ticketNo, body.getOrDefault("reason", "no reason given"));
        return ResponseEntity.ok(Map.of("ticket", ticketNo, "state", "VOID"));
    }

    // ------------------------------------------------------------------

    private static Map<String, Object> toRow(Ticket t) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("ticketNo", t.ticketNo());
        row.put("plate", t.plate());
        row.put("state", t.stateName());
        row.put("slot", t.slotCode());
        row.put("entryTime", t.entryTime().toString());
        row.put("exitTime", t.exitTime() == null ? null : t.exitTime().toString());
        row.put("totalPaid", t.totalPaid().toPlainString());
        return row;
    }

    private Map<String, Object> toDetail(Ticket t) {
        Map<String, Object> detail = new LinkedHashMap<>(toRow(t));
        detail.put("vehicleType", t.vehicleType().name());
        detail.put("accessible", t.accessible());
        detail.put("entryGate", t.entryGateId());
        detail.put("exitGate", t.exitGateId());
        detail.put("tariffPlan", t.tariffPlanId());
        detail.put("tariffExplain", t.tariffExplain());
        detail.put("feeTotal", t.feeTotal().toPlainString());
        detail.put("feeLines", ops.feeLines(t));
        detail.put("payments", t.payments().stream()
                .map(p -> Map.of("method", p.method() == null ? "-" : p.method().label(),
                        "amount", p.amount().toPlainString(),
                        "at", p.at().toString()))
                .toList());
        return detail;
    }
}
