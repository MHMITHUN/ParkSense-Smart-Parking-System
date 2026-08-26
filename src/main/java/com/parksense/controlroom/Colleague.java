package com.parksense.controlroom;

/**
 * A component coordinated by the {@link ParkingMediator} (GoF Mediator —
 * colleague interface). Colleagues never talk to each other; they report
 * to the mediator and receive instructions from it.
 */
public interface Colleague {

    String colleagueId();

    void setMediator(ParkingMediator mediator);
}
