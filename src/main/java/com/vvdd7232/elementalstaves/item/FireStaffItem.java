package com.vvdd7232.elementalstaves.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** A reusable fire charge with a deliberately short cooldown. */
public final class FireStaffItem extends BaseStaffItem {
    private static final int COOLDOWN_TICKS = 24;
    private static final int DURABILITY_COST = 1;

    public FireStaffItem() {
        super(384);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        Vec3 direction = player.getViewVector(1.0F).normalize();
        Vec3 origin = player.getEyePosition().add(direction.scale(0.75D));
        SmallFireball fireball = new SmallFireball(serverLevel, player, direction);
        fireball.setPos(origin.x, origin.y, origin.z);
        fireball.setDeltaMovement(direction.scale(1.15D));
        serverLevel.addFreshEntity(fireball);

        serverLevel.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.9F, 0.9F + serverLevel.getRandom().nextFloat() * 0.2F);
        serverLevel.sendParticles(ParticleTypes.FLAME, origin.x, origin.y, origin.z, 10, 0.08D, 0.08D, 0.08D, 0.02D);
        completeCast(serverPlayer, stack, hand, COOLDOWN_TICKS, DURABILITY_COST);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, net.minecraft.world.item.TooltipFlag tooltipFlag) {
        addDescription(tooltip, "tooltip.elementalstaves.fire_staff.1", "tooltip.elementalstaves.fire_staff.2");
    }
}
