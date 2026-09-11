package com.vvdd7232.elementalstaves.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.List;

public final class LightningStaffItem extends BaseStaffItem {
    public LightningStaffItem() { super(256); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return castSpell(level, player, hand, player.isShiftKeyDown()
                ? StaffMagic.Spell.STORM : StaffMagic.Spell.CHAIN);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        addDescription(tooltip, "tooltip.elementalstaves.lightning_staff.1", "tooltip.elementalstaves.lightning_staff.2");
    }
}
