package com.parksense.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Minimal configuration reader: the process environment wins, and a local
 * {@code .env} file (KEY = VALUE lines, {@code #} comments) is the
 * fallback. Just enough for the two Mongo settings — no library needed.
 */
public final class EnvFile {

    private static final Path ENV_PATH = Path.of(".env");

    private EnvFile() {
    }

    /** Value for a key, or null when neither the environment nor .env has it. */
    public static String get(String key) {
        String fromEnvironment = System.getenv(key);
        if (fromEnvironment != null && !fromEnvironment.isBlank()) {
            return fromEnvironment.trim();
        }
        if (!Files.isRegularFile(ENV_PATH)) {
            return null;
        }
        try {
            for (String rawLine : Files.readAllLines(ENV_PATH)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int split = line.indexOf('=');
                if (split <= 0) {
                    continue;
                }
                if (line.substring(0, split).trim().equals(key)) {
                    String value = line.substring(split + 1).trim();
                    return value.isEmpty() ? null : value;
                }
            }
        } catch (IOException ignored) {
            // unreadable .env behaves like absent .env
        }
        return null;
    }
}
