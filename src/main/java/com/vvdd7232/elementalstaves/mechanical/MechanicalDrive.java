package com.vvdd7232.elementalstaves.mechanical;

import com.vvdd7232.elementalstaves.ElementalStaves;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Bounded straight-line transmission. Never loads chunks and never follows cycles. */
public final class MechanicalDrive {
    public static final int MAX_SHAFTS = DriveLine.MAX_SHAFTS;
    private MechanicalDrive() {}

    public static boolean isDriven(Level level, BlockPos pos, Direction.Axis axis) {
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() != axis) continue;
            if (DriveLine.powered(distance -> {
                BlockPos next = pos.relative(direction, distance);
                if (!level.isInWorldBounds(next) || !level.hasChunkAt(next)) return DriveLine.Node.BLOCKED;
                BlockState state = level.getBlockState(next);
                if (state.is(ElementalStaves.COAL_ENGINE.get())) {
                    return state.getValue(CoalEngineBlock.FACING) == direction.getOpposite()
                            && state.getValue(CoalEngineBlock.LIT)
                            ? DriveLine.Node.POWERED_ENGINE : DriveLine.Node.BLOCKED;
                }
                return state.is(ElementalStaves.DRIVE_SHAFT.get())
                        && state.getValue(DriveShaftBlock.AXIS) == axis
                        ? DriveLine.Node.SHAFT : DriveLine.Node.BLOCKED;
            })) return true;
        }
        return false;
    }
}
