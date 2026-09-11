package com.vvdd7232.elementalstaves.mechanical;

import com.vvdd7232.elementalstaves.ElementalStaves;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Bounded straight-line transmission. Never loads chunks and never follows cycles. */
public final class MechanicalDrive {
    public static final int MAX_SHAFTS = 32;
    private MechanicalDrive() {}

    public static boolean isDriven(Level level, BlockPos pos, Direction.Axis axis) {
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() != axis) continue;
            for (int distance = 1; distance <= MAX_SHAFTS; distance++) {
                BlockPos next = pos.relative(direction, distance);
                if (!level.isInWorldBounds(next) || !level.hasChunkAt(next)) break;
                BlockState state = level.getBlockState(next);
                if (state.is(ElementalStaves.COAL_ENGINE.get())) {
                    if (state.getValue(CoalEngineBlock.FACING) == direction.getOpposite()
                            && state.getValue(CoalEngineBlock.LIT)) return true;
                    break;
                }
                if (!state.is(ElementalStaves.DRIVE_SHAFT.get())
                        || state.getValue(DriveShaftBlock.AXIS) != axis) break;
            }
        }
        return false;
    }
}
