package com.vvdd7232.elementalstaves.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Client-only animation values. Authoritative running state is an ordinary synced block state. */
public abstract class RotatingBlockEntity extends BlockEntity {
    private float angle;
    private boolean animating;
    protected RotatingBlockEntity(BlockEntityType<?> type, BlockPos p, BlockState s) { super(type, p, s); }
    protected void animate(Level level, boolean running) {
        animating = running;
        if (running) angle = (level.getGameTime() % 30) * 12.0F; // 40 RPM, in phase across a line
    }
    public float angle(float partialTick) { return angle + (animating ? partialTick * 12 : 0); }
}
