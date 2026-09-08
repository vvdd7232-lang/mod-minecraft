package com.vvdd7232.elementalstaves.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Air Staff detonates a gust four blocks ahead of the caster: every nearby
 * creature is knocked away from the centre, lifted slightly and given Slow Falling,
 * so nobody takes fall damage. All checks run server-side only.
 */
public final class AirStaffItem extends BaseStaffItem {
    private static final double PUSH_RADIUS = 4.0D;
    private static final int COOLDOWN_TICKS = 50;
    private static final int DURABILITY_COST = 2;

    public AirStaffItem() {
        super(288);
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

        Vec3 look = player.getViewVector(1.0F);
        Vec3 centre = player.getEyePosition().add(look.scale(4.0D));
        int affected = blast(serverLevel, centre);

        if (affected == 0) {
            player.displayClientMessage(Component.translatable("message.elementalstaves.air.no_target"), true);
            return InteractionResultHolder.fail(stack);
        }

        serverLevel.playSound(null, player.blockPosition(), SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 1.0F,
                0.85F + serverLevel.getRandom().nextFloat() * 0.2F);
        serverLevel.sendParticles(ParticleTypes.CLOUD, centre.x, centre.y, centre.z,
                28, 1.2D, 1.2D, 1.2D, 0.05D);
        completeCast(serverPlayer, stack, hand, COOLDOWN_TICKS, DURABILITY_COST);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    /** Pushes living creatures out of {@code centre}. Returns how many were caught in the gust. */
    private static int blast(ServerLevel level, Vec3 centre) {
        int count = 0;
        List<LivingEntity> creatures = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(centre, centre).inflate(PUSH_RADIUS));
        for (LivingEntity creature : creatures) {
            Vec3 to = creature.position().subtract(centre);
            double distance = to.length();
            if (distance < 0.25D || distance > PUSH_RADIUS) {
                continue;
            }
            double falloff = 1.0D - distance / (PUSH_RADIUS + 1.0D);
            double strength = Math.max(0.25D, 1.6D * falloff);
            double horizontal = Math.sqrt(to.x * to.x + to.z * to.z);
            if (horizontal > 1.0E-4D) {
                creature.knockback(strength, to.x / horizontal, to.z / horizontal);
            }
            creature.push(0.0D, Math.max(0.05D, 0.3D * strength), 0.0D);
            creature.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0));
            count++;
        }
        return count;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, net.minecraft.world.item.TooltipFlag tooltipFlag) {
        addDescription(tooltip, "tooltip.elementalstaves.air_staff.1", "tooltip.elementalstaves.air_staff.2");
    }
}
