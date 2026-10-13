package com.eyeofodin;

/** The Eye of Precognition counters for one player. */
public class EyeState {
    public int stacks = EyeLogic.MAX_STACKS;
    public boolean overheated = false;
    public int overheatUntilTick = 0;
    public int lastDodgeTick = -1000;
    public boolean wasEquipped = false;
}
