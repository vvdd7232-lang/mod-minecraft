package com.vvdd7232.elementalstaves.mechanical;

import java.util.function.IntFunction;

/** A shaft can see a matching engine through at most 31 other shafts. */
public final class DriveLine {
    public static final int MAX_SHAFTS = 32;
    public enum Node { SHAFT, POWERED_ENGINE, BLOCKED }
    private DriveLine() {}
    public static boolean powered(IntFunction<Node> lookup) {
        for (int distance = 1; distance <= MAX_SHAFTS; distance++) {
            Node node = lookup.apply(distance);
            if (node == Node.POWERED_ENGINE) return true;
            if (node != Node.SHAFT) return false;
        }
        return false;
    }
}
