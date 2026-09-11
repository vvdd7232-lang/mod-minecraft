package com.vvdd7232.elementalstaves.mechanical;

import com.vvdd7232.elementalstaves.ElementalStaves;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class DriveShaftBlockEntity extends RotatingBlockEntity {
    public DriveShaftBlockEntity(BlockPos p, BlockState s) { super(ElementalStaves.DRIVE_SHAFT_ENTITY.get(), p, s); }
    public static void tick(Level level, BlockPos pos, BlockState state, DriveShaftBlockEntity entity) {
        if (level.isClientSide) { entity.animate(level, state.getValue(DriveShaftBlock.ROTATING)); return; }
        // Polling also handles engine removal and loaded/unloaded chunk boundaries without recursive updates.
        if (level.getGameTime() % 10 != 0) return;
        boolean running = MechanicalDrive.isDriven(level, pos, state.getValue(DriveShaftBlock.AXIS));
        if (running != state.getValue(DriveShaftBlock.ROTATING))
            level.setBlock(pos, state.setValue(DriveShaftBlock.ROTATING, running), Block.UPDATE_CLIENTS);
    }
}
