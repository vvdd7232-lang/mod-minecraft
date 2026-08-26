package com.vvdd7232.elementalstaves.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.List;

/** Shared server-side bookkeeping for every staff cast. */
abstract class BaseStaffItem extends Item {
    protected BaseStaffItem(int durability) {
        super(new FabricItemSettings().maxCount(1).maxDamage(durability));
    }

    /**
     * Applies cooldown, durability and usage statistics after a cast has succeeded.
     * Creative players can cast without consuming durability, like other Minecraft tools.
     */
    protected final void completeCast(PlayerEntity player, ItemStack stack, Hand hand, int cooldownTicks, int durabilityCost) {
        player.getItemCooldownManager().set(this, cooldownTicks);
        player.incrementStat(Stats.USED.getOrCreateStat(this));

        if (!player.getAbilities().creativeMode) {
            stack.damage(durabilityCost, player, caster -> caster.sendToolBreakStatus(hand));
        }
    }

    protected final void addDescription(List<Text> tooltip, String firstLineKey, String secondLineKey) {
        tooltip.add(Text.translatable(firstLineKey).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable(secondLineKey).formatted(Formatting.DARK_GRAY));
    }
}
