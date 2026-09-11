package com.vvdd7232.elementalstaves.mechanical;

import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Identical fuel interaction and data in both optional backends. */
public interface EngineAccess {
    EngineFuel engineFuel();
    default BlockEntity engineEntity() { return (BlockEntity) this; }
    default boolean insert(Player player, ItemStack stack) {
        if (!stack.is(Items.COAL) && !stack.is(Items.CHARCOAL)) return false;
        boolean isCharcoal = stack.is(Items.CHARCOAL);
        int count = engineFuel().insert(isCharcoal, player.isShiftKeyDown() ? stack.getCount() : 1);
        if (count == 0) {
            player.displayClientMessage(Component.translatable("message.elementalstaves.engine.full"), true);
            return false;
        }
        if (!player.getAbilities().instabuild) stack.shrink(count);
        engineEntity().setChanged();
        status(player);
        return true;
    }

    default void interactEmpty(Player player) {
        if (player.isShiftKeyDown() && engineFuel().queued() > 0) {
            ItemStack extracted = new ItemStack(engineFuel().charcoal() ? Items.CHARCOAL : Items.COAL, engineFuel().queued());
            engineFuel().extract();
            engineEntity().setChanged();
            player.getInventory().placeItemBackInInventory(extracted);
        }
        status(player);
    }

    private void status(Player player) {
        boolean stopped = engineEntity().getLevel() != null && engineEntity().getLevel().hasNeighborSignal(engineEntity().getBlockPos());
        player.displayClientMessage(Component.translatable("message.elementalstaves.engine.status",
                engineFuel().queued(), (engineFuel().remaining() + 19) / 20, Component.translatable(stopped
                        ? "message.elementalstaves.engine.paused" : engineFuel().remaining() > 0
                        ? "message.elementalstaves.engine.running" : "message.elementalstaves.engine.idle")), true);
    }

    default void dropFuel() {
        if (engineEntity().getLevel() != null && !engineEntity().getLevel().isClientSide && engineFuel().queued() > 0) {
            Containers.dropItemStack(engineEntity().getLevel(), engineEntity().getBlockPos().getX() + 0.5, engineEntity().getBlockPos().getY() + 0.5,
                    engineEntity().getBlockPos().getZ() + 0.5, new ItemStack(engineFuel().charcoal() ? Items.CHARCOAL : Items.COAL, engineFuel().queued()));
            engineFuel().extract();
        }
    }

}
