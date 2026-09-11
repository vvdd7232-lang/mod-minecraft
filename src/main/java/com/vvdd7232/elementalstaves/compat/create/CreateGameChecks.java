package com.vvdd7232.elementalstaves.compat.create;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.vvdd7232.elementalstaves.ElementalStaves;
import com.vvdd7232.elementalstaves.mechanical.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Referenced only from the Create branch of a GameTest. No annotations to load it without Create. */
public final class CreateGameChecks {
    private CreateGameChecks() {}
    public static void assertSpeed(GameTestHelper h, BlockPos p, float expected) {
        h.assertTrue(h.getBlockEntity(p) instanceof KineticBlockEntity, "Not a native Create block entity");
        float actual = Math.abs(((KineticBlockEntity)h.getBlockEntity(p)).getSpeed());
        h.assertTrue(Math.abs(actual - expected) < 0.01, "Expected " + expected + " RPM, got " + actual);
    }
    public static void networkAndStress(GameTestHelper h) {
        BlockPos engine = new BlockPos(1,1,1), shaft = new BlockPos(2,1,1), gearbox = new BlockPos(3,1,1);
        BlockPos turn = new BlockPos(3,1,2), press = new BlockPos(3,1,3);
        h.setBlock(engine, ElementalStaves.COAL_ENGINE.get().defaultBlockState().setValue(CoalEngineBlock.FACING, Direction.EAST));
        h.setBlock(shaft, AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
        h.setBlock(gearbox, AllBlocks.GEARBOX.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
        h.setBlock(turn, ElementalStaves.DRIVE_SHAFT.get().defaultBlockState().setValue(DriveShaftBlock.AXIS, Direction.Axis.Z));
        ((EngineAccess)h.getBlockEntity(engine)).engineFuel().insert(false, 2);
        double normalImpact = BlockStressValues.getImpact(AllBlocks.MECHANICAL_PRESS.get());
        java.util.concurrent.atomic.AtomicReference<Double> testImpact = new java.util.concurrent.atomic.AtomicReference<>(100.0);
        h.runAfterDelay(40, () -> {
            assertSpeed(h, shaft, 40); assertSpeed(h, turn, 40);
            h.assertTrue(((CreateEngineEntity)h.getBlockEntity(engine)).calculateAddedStressCapacity() == 64, "Wrong engine capacity");
            // Simulate a configured high-impact machine using Create's public stress registry.
            BlockStressValues.IMPACTS.register(AllBlocks.MECHANICAL_PRESS.get(), () -> testImpact.get());
            h.setBlock(press, AllBlocks.MECHANICAL_PRESS.getDefaultState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        });
        h.runAfterDelay(80, () -> {
            assertSpeed(h, turn, 0);
            h.assertTrue(((KineticBlockEntity)h.getBlockEntity(engine)).isOverStressed(), "Overload must stop network");
            h.setBlock(press, Blocks.AIR);
            testImpact.set(normalImpact);
        });
        h.runAfterDelay(120, () -> {
            assertSpeed(h, turn, 40);
            // Reverse powering: a native Create motor must be able to drive our shaft as well.
            h.setBlock(engine, Blocks.AIR);
            h.setBlock(shaft, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(BlockStateProperties.FACING, Direction.EAST));
        });
        h.runAfterDelay(170, () -> { assertSpeed(h, turn, 16); h.succeed(); });
    }
}
