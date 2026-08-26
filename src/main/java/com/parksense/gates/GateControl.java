package com.parksense.gates;

/**
 * Emergency and maintenance control of a lane barrier. Normal cycling
 * happens through command objects; this interface is the manual override
 * surface, which is exactly why it sits behind a protection proxy.
 */
public interface GateControl {

    /** Force the barrier up and hold it (emergency exit, maintenance). */
    void forceOpen(String actor);

    /** Force the barrier down and hold it. */
    void forceClose(String actor);

    /** Return the lane to automatic command-driven cycling. */
    void resumeAutomatic(String actor);
}
