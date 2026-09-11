package com.vvdd7232.elementalstaves.mechanical;

/** Pure fuel accounting, independently testable without a Minecraft world. */
public final class EngineFuel {
    public static final int BURN_TICKS = 1600;
    private int queued;
    private boolean charcoal;
    private int remaining;
    public int queued() { return queued; }
    public boolean charcoal() { return charcoal; }
    public int remaining() { return remaining; }
    public int insert(boolean type, int requested) {
        if (queued > 0 && charcoal != type) return 0;
        int accepted = Math.clamp(requested, 0, 64 - queued);
        if (accepted > 0) { queued += accepted; charcoal = type; }
        return accepted;
    }
    public int extract() {
        int result = queued;
        queued = 0;
        return result;
    }
    public boolean tick(boolean paused) {
        if (paused) return false;
        if (remaining == 0 && queued > 0) { queued--; remaining = BURN_TICKS; }
        if (remaining == 0) return false;
        remaining--;
        return true;
    }
    public void restore(int count, boolean type, int ticks) {
        queued = Math.clamp(count, 0, 64);
        charcoal = type;
        remaining = Math.clamp(ticks, 0, BURN_TICKS);
    }
}
