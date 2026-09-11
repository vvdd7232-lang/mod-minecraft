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
    private final EngineFuel fuel = new EngineFuel();
    private long lastServerTick = -1;

    public CoalEngineBlockEntity(BlockPos p, BlockState s) { super(ElementalStaves.COAL_ENGINE_ENTITY.get(), p, s); }

    public boolean insert(Player player, ItemStack stack) {
        if (!stack.is(Items.COAL) && !stack.is(Items.CHARCOAL)) return false;
        boolean isCharcoal = stack.is(Items.CHARCOAL);
        int count = fuel.insert(isCharcoal, player.isShiftKeyDown() ? stack.getCount() : 1);
        if (count == 0) {
            player.displayClientMessage(Component.translatable("message.elementalstaves.engine.full"), true);
            return false;
        }
        if (!player.getAbilities().instabuild) stack.shrink(count);
        setChanged();
        status(player);
        return true;
    }

    public void interactEmpty(Player player) {
        if (player.isShiftKeyDown() && fuel.queued() > 0) {
            ItemStack extracted = new ItemStack(fuel.charcoal() ? Items.CHARCOAL : Items.COAL, fuel.queued());
            fuel.extract();
            setChanged();
            player.getInventory().placeItemBackInInventory(extracted);
        }
        status(player);
    }

    private void status(Player player) {
        boolean stopped = level != null && level.hasNeighborSignal(worldPosition);
        player.displayClientMessage(Component.translatable("message.elementalstaves.engine.status",
                fuel.queued(), (fuel.remaining() + 19) / 20, Component.translatable(stopped
                        ? "message.elementalstaves.engine.paused" : fuel.remaining() > 0
                        ? "message.elementalstaves.engine.running" : "message.elementalstaves.engine.idle")), true);
    }

    public void dropFuel() {
        if (level != null && !level.isClientSide && fuel.queued() > 0) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5, new ItemStack(fuel.charcoal() ? Items.CHARCOAL : Items.COAL, fuel.queued()));
            fuel.extract();
        }
    }

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
