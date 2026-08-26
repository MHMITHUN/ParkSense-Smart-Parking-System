package com.parksense.api;

import com.parksense.app.OperationsFacade;
import com.parksense.reports.CsvRenderer;
import com.parksense.reports.ReportResult;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Report endpoints — every report is a visitor run, rendered as JSON or
 * CSV from the same {@link ReportResult}.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final OperationsFacade ops;

    public ReportController(OperationsFacade ops) {
        this.ops = ops;
    }

    @GetMapping("/dashboard")
    public Object dashboard() {
        return ops.dashboard();
    }

    @GetMapping("/{name}")
    public ResponseEntity<Object> report(@PathVariable String name,
                                         @RequestParam(required = false) String from,
                                         @RequestParam(required = false) String to,
                                         @RequestParam(defaultValue = "json") String format) {
        LocalDate today = LocalDate.now();
        LocalDate fromDate = from == null ? today.minusDays(7) : LocalDate.parse(from);
        LocalDate toDate = to == null ? today.plusDays(1) : LocalDate.parse(to).plusDays(1);
        Instant fromInstant = fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant toInstant = toDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

        ReportResult result = ops.runReport(name, fromInstant, toInstant);
        if ("csv".equalsIgnoreCase(format)) {
            String filename = "parksense-" + name + ".csv";
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=" + filename)
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(CsvRenderer.render(result));
        }
        return ResponseEntity.ok(result);
    }
}
