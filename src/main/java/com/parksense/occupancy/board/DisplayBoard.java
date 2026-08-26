package com.parksense.occupancy.board;

import com.parksense.occupancy.OccupancyListener;

/**
 * A physical LED board. Boards are {@code OccupancyListener}s (GoF
 * Observer): they re-render themselves whenever a slot changes state, and
 * the dashboard reads the latest {@link DisplaySnapshot} at any time.
 */
public interface DisplayBoard extends OccupancyListener {

    String boardId();

    String title();

    DisplaySnapshot render();
}
