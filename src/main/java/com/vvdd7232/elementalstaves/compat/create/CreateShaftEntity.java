package com.vvdd7232.elementalstaves.compat.create;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.vvdd7232.elementalstaves.mechanical.MechanicalPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

public final class CreateShaftEntity extends KineticBlockEntity {
    public CreateShaftEntity(BlockPos p, BlockState s) { super(MechanicalPlatform.shaftType(), p, s); }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        super.addToGoggleTooltip(tooltip, sneaking);
        tooltip.add(Component.translatable("tooltip.elementalstaves.create.speed", Math.abs(getSpeed())));
        return true;
    }
}
