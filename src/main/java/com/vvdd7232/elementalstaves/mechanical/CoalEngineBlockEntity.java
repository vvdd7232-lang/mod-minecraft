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
public final class CoalEngineBlockEntity extends RotatingBlockEntity {
    public static final int BURN_TICKS = 1600;
    private int fuelCount;
    private boolean charcoal;
    private int burnTicks;

    public CoalEngineBlockEntity(BlockPos p, BlockState s) { super(ElementalStaves.COAL_ENGINE_ENTITY.get(), p, s); }

    public boolean insert(Player player, ItemStack stack) {
        boolean isCharcoal = stack.is(Items.CHARCOAL);
        if (fuelCount >= 64 || (fuelCount > 0 && charcoal != isCharcoal)) {
            player.displayClientMessage(Component.translatable("message.elementalstaves.engine.full"), true);
            return false;
        }
        int count = Math.min(64 - fuelCount, player.isShiftKeyDown() ? stack.getCount() : 1);
        fuelCount += count;
        charcoal = isCharcoal;
        if (!player.getAbilities().instabuild) stack.shrink(count);
        setChanged();
        status(player);
        return true;
    }

    public void interactEmpty(Player player) {
        if (player.isShiftKeyDown() && fuelCount > 0) {
            ItemStack extracted = new ItemStack(charcoal ? Items.CHARCOAL : Items.COAL, fuelCount);
            fuelCount = 0;
            setChanged();
            player.getInventory().placeItemBackInInventory(extracted);
        }
        status(player);
    }

    private void status(Player player) {
        boolean stopped = level != null && level.hasNeighborSignal(worldPosition);
        player.displayClientMessage(Component.translatable("message.elementalstaves.engine.status",
                fuelCount, (burnTicks + 19) / 20, Component.translatable(stopped
                        ? "message.elementalstaves.engine.paused" : burnTicks > 0
                        ? "message.elementalstaves.engine.running" : "message.elementalstaves.engine.idle")), true);
    }

    public void dropFuel() {
        if (level != null && !level.isClientSide && fuelCount > 0) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5, new ItemStack(charcoal ? Items.CHARCOAL : Items.COAL, fuelCount));
            fuelCount = 0;
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CoalEngineBlockEntity entity) {
        if (level.isClientSide) { entity.animate(level, state.getValue(CoalEngineBlock.LIT)); return; }
        boolean running = false;
        if (!level.hasNeighborSignal(pos)) {
            if (entity.burnTicks == 0 && entity.fuelCount > 0) {
                entity.fuelCount--;
                entity.burnTicks = BURN_TICKS;
            }
            if (entity.burnTicks > 0) {
                running = true;
                entity.burnTicks--;
                entity.setChanged();
            }
        }
        if (running != state.getValue(CoalEngineBlock.LIT))
            level.setBlock(pos, state.setValue(CoalEngineBlock.LIT, running), Block.UPDATE_ALL);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Fuel", fuelCount);
        tag.putBoolean("Charcoal", charcoal);
        tag.putInt("BurnTicks", burnTicks);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuelCount = Math.clamp(tag.getInt("Fuel"), 0, 64);
        charcoal = tag.getBoolean("Charcoal");
        burnTicks = Math.clamp(tag.getInt("BurnTicks"), 0, BURN_TICKS);
    }
}
