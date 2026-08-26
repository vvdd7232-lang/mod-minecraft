package com.vvdd7232.elementalstaves.item;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Shared, authoritative server-side bookkeeping for every staff cast. */
abstract class BaseStaffItem extends Item {
    protected BaseStaffItem(int durability) {
        super(new Item.Properties().stacksTo(1).durability(durability));
    }

    /** Applies cooldown, durability and the vanilla usage statistic after a successful cast. */
    protected final void completeCast(ServerPlayer player, ItemStack stack, InteractionHand hand, int cooldownTicks, int durabilityCost) {
        player.getCooldowns().addCooldown(this, cooldownTicks);
        player.awardStat(Stats.ITEM_USED.get(this));

        if (!player.getAbilities().instabuild) {
            EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            stack.hurtAndBreak(durabilityCost, player, slot);
        }
    }

    protected final void addDescription(List<Component> tooltip, String firstLineKey, String secondLineKey) {
        tooltip.add(Component.translatable(firstLineKey).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(secondLineKey).withStyle(ChatFormatting.DARK_GRAY));
    }
}
