package com.parksense.api;

import com.parksense.api.dto.GateDtos.EntryRequest;
import com.parksense.api.dto.GateDtos.ExitRequestDto;
import com.parksense.api.dto.GateDtos.UndoResponse;
import com.parksense.app.OperationsFacade;
import com.parksense.tickets.PaymentMethod;
import com.parksense.vehicles.VehicleType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Lane endpoints: the gate simulator's backend. Entry runs the camera →
 * chain → mediator path; exit runs the template-method lane processors.
 */
@RestController
@RequestMapping("/api/gates")
public class GateController {

    private final OperationsFacade ops;

    public GateController(OperationsFacade ops) {
        this.ops = ops;
    }

    @GetMapping
    public List<Map<String, Object>> gates() {
        return ops.gatePanels();
    }

    @GetMapping("/{id}")
    public Map<String, Object> gate(@PathVariable String id) {
        return ops.gatePanel(id);
    }

    @PostMapping("/{id}/entry")
    public Object entry(@PathVariable String id, @RequestBody EntryRequest request) {
        return ops.simulateEntry(id, request.plate(),
                VehicleType.valueOf(request.vehicleType().toUpperCase()),
                request.accessible());
    }

    @PostMapping("/{id}/exit")
    public Object exit(@PathVariable String id, @RequestBody ExitRequestDto request) {
        PaymentMethod method = request.method() == null ? null
                : PaymentMethod.valueOf(request.method().toUpperCase());
        return ops.requestExit(id, request.reference(), method, request.tendered(),
                request.lostTicket());
    }

    @PostMapping("/{id}/commands/{commandId}/undo")
    public UndoResponse undo(@PathVariable String id, @PathVariable String commandId) {
        return new UndoResponse(id, commandId, ops.undoGateCommand(id, commandId));
    }
}
