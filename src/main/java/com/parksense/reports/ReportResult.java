package com.parksense.reports;

import java.time.Instant;
import java.util.List;

/**
 * One rendered report: a titled table plus an optional numeric series for
 * charting. Deliberately generic — every report type fills the same shape,
 * so CSV export and the SPA render one format.
 */
public record ReportResult(
        String name,
        Instant generatedAt,
        String from,
        String to,
        List<String> columns,
        List<List<String>> rows,
        List<String> seriesLabels,
        List<Double> seriesValues) {

    public static ReportResult table(String name, String from, String to,
                                     List<String> columns, List<List<String>> rows) {
        return new ReportResult(name, Instant.now(), from, to, columns, rows, null, null);
    }

    public static ReportResult chart(String name, String from, String to,
                                     List<String> columns, List<List<String>> rows,
                                     List<String> labels, List<Double> values) {
        return new ReportResult(name, Instant.now(), from, to, columns, rows, labels, values);
    }
}
