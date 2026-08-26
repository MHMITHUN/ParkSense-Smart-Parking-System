package com.parksense.reports;

import java.util.List;

/** RFC-4180-ish CSV rendering of any {@link ReportResult}. */
public final class CsvRenderer {

    private CsvRenderer() {
    }

    public static String render(ReportResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", result.columns())).append("\r\n");
        for (List<String> row : result.rows()) {
            sb.append(row.stream().map(CsvRenderer::cell).reduce((a, b) -> a + "," + b).orElse(""))
                    .append("\r\n");
        }
        return sb.toString();
    }

    private static String cell(String value) {
        if (value == null) {
            return "";
        }
        String safe = value.replace("\"", "\"\"");
        return safe.contains(",") || safe.contains("\"") || safe.contains("\n")
                ? "\"" + safe + "\"" : safe;
    }
}
