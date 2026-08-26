package com.parksense.api;

import com.parksense.app.OperationsFacade;
import com.parksense.auth.RequestRoles;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Emergency barrier control. Every call flows through the protection
 * proxy, so a non-ADMIN is refused with 403 and an audit line — the UI
 * demo of the Proxy pattern.
 */
@RestController
@RequestMapping("/api/control")
public class ControlController {

    private final OperationsFacade ops;

    public ControlController(OperationsFacade ops) {
        this.ops = ops;
    }

    @PostMapping("/gates/{id}/force-open")
    public ResponseEntity<Object> forceOpen(@PathVariable String id) {
        ops.forceBarrier(id, "OPEN");
        return ResponseEntity.ok(Map.of("ok", true, "action", "force-open", "gate", id));
    }

    @PostMapping("/gates/{id}/force-close")
    public ResponseEntity<Object> forceClose(@PathVariable String id) {
        ops.forceBarrier(id, "CLOSE");
        return ResponseEntity.ok(Map.of("ok", true, "action", "force-close", "gate", id));
    }

    @PostMapping("/gates/{id}/resume")
    public ResponseEntity<Object> resume(@PathVariable String id) {
        ops.forceBarrier(id, "RESUME");
        return ResponseEntity.ok(Map.of("ok", true, "action", "resume", "gate", id));
    }
}
