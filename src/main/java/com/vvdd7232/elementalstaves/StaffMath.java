package com.vvdd7232.elementalstaves;

/**
 * Pure geometry helpers for staff range checks.
 * Kept free of Minecraft types so the same numbers can be unit-tested.
 */
public final class StaffMath {
    private StaffMath() {
    }

    public static double squaredDelta(double dx, double dy, double dz) {
        return dx * dx + dy * dy + dz * dz;
    }

    public static double squaredBlockDistance(int x1, int y1, int z1, int x2, int y2, int z2) {
        return squaredDelta((double) x1 - x2, (double) y1 - y2, (double) z1 - z2);
    }

    public static boolean withinSquaredRange(double squaredDistance, double range) {
        return squaredDistance <= range * range;
    }
}
