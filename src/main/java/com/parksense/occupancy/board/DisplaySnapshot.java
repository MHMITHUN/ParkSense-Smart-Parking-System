package com.parksense.occupancy.board;

import java.time.Instant;
import java.util.List;

/**
 * What a display board currently shows. Snapshots are immutable; the API
 * serialises them directly to the dashboard so the web UI renders exactly
 * the same lines a driver would read on the physical LED board.
 */
public record DisplaySnapshot(String boardId, String title, List<String> lines, Instant updatedAt) {
}
