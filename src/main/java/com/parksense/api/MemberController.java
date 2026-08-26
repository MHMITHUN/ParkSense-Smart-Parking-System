package com.parksense.api;

import com.parksense.app.OperationsFacade;
import com.parksense.auth.RequestRoles;
import com.parksense.members.Member;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Member registry CRUD (writes are ADMIN-only). */
@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final OperationsFacade ops;

    public MemberController(OperationsFacade ops) {
        this.ops = ops;
    }

    @GetMapping
    public List<Map<String, Object>> members() {
        return ops.members().stream().map(MemberController::toRow).toList();
    }

    @PostMapping
    public ResponseEntity<Object> create(@RequestBody Map<String, Object> body) {
        if (!RequestRoles.isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Only ADMIN may edit members"));
        }
        Member member = new Member(
                "MEM-" + String.format("%03d", ops.members().size() + 1),
                String.valueOf(body.getOrDefault("name", "Unnamed")),
                String.valueOf(body.getOrDefault("phone", "")),
                plates(body),
                String.valueOf(body.getOrDefault("plan", "MONTHLY")),
                LocalDate.parse(String.valueOf(body.getOrDefault("validUntil",
                        LocalDate.now().plusMonths(1).toString()))));
        return ResponseEntity.ok(toRow(ops.saveMember(member)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> update(@PathVariable String id,
                                         @RequestBody Map<String, Object> body) {
        if (!RequestRoles.isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Only ADMIN may edit members"));
        }
        Member existing = ops.members().stream()
                .filter(m -> m.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No member " + id));
        Member updated = new Member(existing.id(),
                String.valueOf(body.getOrDefault("name", existing.name())),
                String.valueOf(body.getOrDefault("phone", existing.phone())),
                body.containsKey("plates") ? plates(body) : existing.plates(),
                String.valueOf(body.getOrDefault("plan", existing.planName())),
                body.containsKey("validUntil")
                        ? LocalDate.parse(String.valueOf(body.get("validUntil")))
                        : existing.validUntil());
        return ResponseEntity.ok(toRow(ops.saveMember(updated)));
    }

    @SuppressWarnings("unchecked")
    private static Set<String> plates(Map<String, Object> body) {
        Object raw = body.get("plates");
        Set<String> plates = new LinkedHashSet<>();
        if (raw instanceof List<?> list) {
            list.forEach(p -> plates.add(String.valueOf(p)));
        } else if (raw instanceof String s) {
            for (String part : s.split("[,\\s]+")) {
                if (!part.isBlank()) {
                    plates.add(part);
                }
            }
        }
        return plates;
    }

    private static Map<String, Object> toRow(Member m) {
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("id", m.id());
        row.put("name", m.name());
        row.put("phone", m.phone());
        row.put("plates", m.plates());
        row.put("plan", m.planName());
        row.put("validUntil", m.validUntil().toString());
        row.put("active", m.active());
        row.put("expired", m.validUntil().isBefore(LocalDate.now()));
        return row;
    }
}
