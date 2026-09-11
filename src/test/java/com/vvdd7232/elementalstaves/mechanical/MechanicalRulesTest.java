package com.vvdd7232.elementalstaves.mechanical;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class MechanicalRulesTest {
    @Test void oneCoalRunsForExactlyEightySeconds() {
        EngineFuel fuel = new EngineFuel();
        assertEquals(1, fuel.insert(false, 1));
        for (int i = 0; i < 1600; i++) assertTrue(fuel.tick(false));
        assertFalse(fuel.tick(false));
        assertEquals(0, fuel.queued());
    }
    @Test void redstonePausesBothIgnitionAndBurning() {
        EngineFuel fuel = new EngineFuel();
        fuel.insert(true, 2);
        for (int i = 0; i < 100; i++) assertFalse(fuel.tick(true));
        assertEquals(2, fuel.queued());
        assertEquals(0, fuel.remaining());
        assertTrue(fuel.tick(false));
        assertEquals(1599, fuel.remaining());
        for (int i = 0; i < 100; i++) assertFalse(fuel.tick(true));
        assertEquals(1599, fuel.remaining());
        assertEquals(1, fuel.queued());
    }
    @Test void slotCapsAtSixtyFourAndRejectsMixedFuel() {
        EngineFuel fuel = new EngineFuel();
        assertEquals(64, fuel.insert(true, 100));
        assertEquals(0, fuel.insert(false, 1));
        assertEquals(0, fuel.insert(true, 1));
        assertEquals(64, fuel.extract());
        assertEquals(0, fuel.extract());
        assertEquals(1, fuel.insert(false, 1));
    }
    @Test void extractionNeverRefundsBurningCoal() {
        EngineFuel fuel = new EngineFuel();
        fuel.insert(false, 2);
        fuel.tick(false);
        assertEquals(1, fuel.extract());
        assertEquals(1599, fuel.remaining());
        for (int i = 0; i < 1599; i++) assertTrue(fuel.tick(false));
        assertFalse(fuel.tick(false));
    }
    @Test void restoreKeepsBurnProgressAndClampsInvalidData() {
        EngineFuel original = new EngineFuel();
        original.insert(true, 10);
        original.tick(false);
        EngineFuel loaded = new EngineFuel();
        loaded.restore(original.queued(), original.charcoal(), original.remaining());
        assertEquals(9, loaded.queued());
        assertTrue(loaded.charcoal());
        assertEquals(1599, loaded.remaining());
        loaded.restore(-20, false, -1);
        assertEquals(0, loaded.queued());
        assertEquals(0, loaded.remaining());
        loaded.restore(500, false, 50000);
        assertEquals(64, loaded.queued());
        assertEquals(1600, loaded.remaining());
    }
    @Test void connectedEngineAtLimitPowersLine() {
        assertTrue(DriveLine.powered(d -> d == 1 ? DriveLine.Node.POWERED_ENGINE : DriveLine.Node.BLOCKED));
        assertTrue(DriveLine.powered(d -> d == 32 ? DriveLine.Node.POWERED_ENGINE : DriveLine.Node.SHAFT));
    }
    @Test void disconnectedOrOverlengthLinesDoNotRotate() {
        assertFalse(DriveLine.powered(d -> d == 33 ? DriveLine.Node.POWERED_ENGINE : DriveLine.Node.SHAFT));
        assertFalse(DriveLine.powered(d -> d == 2 ? DriveLine.Node.BLOCKED : d == 3 ? DriveLine.Node.POWERED_ENGINE : DriveLine.Node.SHAFT));
    }
    @Test void obstructionOrUnloadedChunkStopsFurtherLookups() {
        AtomicInteger queries = new AtomicInteger();
        assertFalse(DriveLine.powered(d -> {
            queries.incrementAndGet();
            return DriveLine.Node.BLOCKED;
        }));
        assertEquals(1, queries.get());
        queries.set(0);
        assertFalse(DriveLine.powered(d -> { queries.incrementAndGet(); return DriveLine.Node.SHAFT; }));
        assertEquals(32, queries.get());
    }
}
