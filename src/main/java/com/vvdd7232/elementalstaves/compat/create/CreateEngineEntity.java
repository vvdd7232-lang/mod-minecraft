package com.vvdd7232.elementalstaves.compat.create;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.vvdd7232.elementalstaves.mechanical.CoalEngineBlock;
import com.vvdd7232.elementalstaves.mechanical.EngineAccess;
import com.vvdd7232.elementalstaves.mechanical.EngineFuel;
import com.vvdd7232.elementalstaves.mechanical.MechanicalPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

public final class CreateEngineEntity extends GeneratingKineticBlockEntity implements EngineAccess {
    private final EngineFuel fuel = new EngineFuel();
    private float previousGeneration = Float.NaN;
    public CreateEngineEntity(BlockPos p, BlockState s) { super(MechanicalPlatform.engineType(), p, s); }
    @Override public EngineFuel engineFuel() { return fuel; }
    @Override public float getGeneratedSpeed() {
        return getBlockState().getValue(CoalEngineBlock.LIT)
                ? convertToDirection(40, getBlockState().getValue(CoalEngineBlock.FACING)) : 0;
    }
    @Override public void tick() {
        super.tick();
        if (level == null || level.isClientSide || isVirtual() || isRemoved()) return;
        boolean running = fuel.tick(level.hasNeighborSignal(worldPosition));
        if (running) setChanged();
        BlockState state = getBlockState();
        if (running != state.getValue(CoalEngineBlock.LIT))
            level.setBlock(worldPosition, state.setValue(CoalEngineBlock.LIT, running), Block.UPDATE_ALL);
        float generated = getGeneratedSpeed();
        if (generated != previousGeneration) {
            previousGeneration = generated;
            updateGeneratedRotation();
        }
        if (level.getGameTime() % 20 == 0 && running) sendData(); // goggles fuel counter
    }
    @Override protected void write(CompoundTag tag, HolderLookup.Provider lookup, boolean clientPacket) {
        super.write(tag, lookup, clientPacket);
        tag.putInt("Fuel", fuel.queued()); tag.putInt("BurnTicks", fuel.remaining()); tag.putBoolean("Charcoal", fuel.charcoal());
    }
    @Override protected void read(CompoundTag tag, HolderLookup.Provider lookup, boolean clientPacket) {
        super.read(tag, lookup, clientPacket);
        fuel.restore(tag.getInt("Fuel"), tag.getBoolean("Charcoal"), tag.getInt("BurnTicks"));
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        super.addToGoggleTooltip(tooltip, sneaking);
        tooltip.add(Component.translatable("tooltip.elementalstaves.create.speed", Math.abs(getSpeed())));
        tooltip.add(Component.translatable("tooltip.elementalstaves.create.fuel", fuel.queued(), (fuel.remaining()+19)/20));
        return true;
    }
}
