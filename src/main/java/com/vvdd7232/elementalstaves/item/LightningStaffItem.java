package com.vvdd7232.elementalstaves.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/** Calls down one real lightning bolt on the block the caster is looking at. */
public final class LightningStaffItem extends BaseStaffItem {
    private static final double RANGE = 32.0D;
    private static final int COOLDOWN_TICKS = 80;
    private static final int DURABILITY_COST = 2;

    public LightningStaffItem() {
        super(256);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        HitResult hit = player.pick(RANGE, 1.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            player.displayClientMessage(Component.translatable("message.elementalstaves.lightning.no_target"), true);
            return InteractionResultHolder.fail(stack);
        }

        BlockPos target = ((BlockHitResult) hit).getBlockPos();
        if (!serverLevel.mayInteract(serverPlayer, target)) {
            player.displayClientMessage(Component.translatable("message.elementalstaves.common.protected"), true);
            return InteractionResultHolder.fail(stack);
        }

        LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, serverLevel);
        lightning.setPos(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        lightning.setCause(serverPlayer);
        serverLevel.addFreshEntity(lightning);

        serverLevel.playSound(null, target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.75F, 1.0F);
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX() + 0.5D, target.getY() + 1.0D, target.getZ() + 0.5D, 18, 0.35D, 0.35D, 0.35D, 0.12D);
        completeCast(serverPlayer, stack, hand, COOLDOWN_TICKS, DURABILITY_COST);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, net.minecraft.world.item.TooltipFlag tooltipFlag) {
        addDescription(tooltip, "tooltip.elementalstaves.lightning_staff.1", "tooltip.elementalstaves.lightning_staff.2");
    }
}
