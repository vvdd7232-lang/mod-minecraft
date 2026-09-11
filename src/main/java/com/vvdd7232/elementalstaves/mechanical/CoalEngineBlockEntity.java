package com.vvdd7232.elementalstaves.mechanical;

import com.vvdd7232.elementalstaves.ElementalStaves;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** One coal/charcoal slot, no menu. Burn progress pauses under a redstone signal. */
public final class CoalEngineBlockEntity extends RotatingBlockEntity implements EngineAccess {
    private final EngineFuel fuel = new EngineFuel();
    private long lastServerTick = -1;

    public CoalEngineBlockEntity(BlockPos p, BlockState s) { super(ElementalStaves.COAL_ENGINE_ENTITY.get(), p, s); }

    @Override public EngineFuel engineFuel() { return fuel; }

    public static void tick(Level level, BlockPos pos, BlockState state, CoalEngineBlockEntity entity) {
        if (level.isClientSide) { entity.animate(level, state.getValue(CoalEngineBlock.LIT)); return; }
        entity.lastServerTick = level.getGameTime();
        boolean running = entity.fuel.tick(level.hasNeighborSignal(pos));
        if (running) entity.setChanged();
        if (running != state.getValue(CoalEngineBlock.LIT))
            level.setBlock(pos, state.setValue(CoalEngineBlock.LIT, running), Block.UPDATE_ALL);
    }

    public boolean isTicking() {
        return level != null && lastServerTick >= 0 && level.getGameTime() - lastServerTick <= 1;
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Fuel", fuel.queued());
        tag.putBoolean("Charcoal", fuel.charcoal());
        tag.putInt("BurnTicks", fuel.remaining());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuel.restore(tag.getInt("Fuel"), tag.getBoolean("Charcoal"), tag.getInt("BurnTicks"));
    }
}
