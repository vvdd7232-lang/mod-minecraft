package com.vvdd7232.elementalstaves.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.List;

public final class LifeStaffItem extends BaseStaffItem {
    public LifeStaffItem() { super(300); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return castSpell(level, player, hand, player.isShiftKeyDown()
                ? StaffMagic.Spell.SANCTUARY : StaffMagic.Spell.HEAL);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        addDescription(tooltip, "tooltip.elementalstaves.life_staff.1", "tooltip.elementalstaves.life_staff.2");
    }

    @Override
    public net.minecraft.world.InteractionResult interactLivingEntity(ItemStack stack, Player player,
            net.minecraft.world.entity.LivingEntity target, InteractionHand hand) {
        if (player.isShiftKeyDown()) return use(player.level(), player, hand).getResult();
        if (player.isSpectator() || player.getCooldowns().isOnCooldown(this))
            return net.minecraft.world.InteractionResult.FAIL;
        if (player.level().isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
        if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)
                || !(player.level() instanceof net.minecraft.server.level.ServerLevel sl))
            return net.minecraft.world.InteractionResult.FAIL;
        if (!StaffMagic.heal(sl, target, 12)) {
            player.displayClientMessage(Component.translatable("message.elementalstaves.spell.no_effect"), true);
            return net.minecraft.world.InteractionResult.FAIL;
        }
        completeCast(sp, stack, hand, 30, 4);
        return net.minecraft.world.InteractionResult.CONSUME;
    }
}
