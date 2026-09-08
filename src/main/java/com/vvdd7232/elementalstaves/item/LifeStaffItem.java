package com.vvdd7232.elementalstaves.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * The Staff of Life restores health and removes every harmful status effect from
 * the targeted creature. Right-clicking a creature heals it, right-clicking air
 * heals the caster. All effects run server-side only.
 */
public final class LifeStaffItem extends BaseStaffItem {
    private static final int COOLDOWN_TICKS = 70;
    private static final int SELF_DURABILITY_COST = 4;
    private static final int OTHER_DURABILITY_COST = 6;
    private static final float SELF_HEAL = 8.0F;
    private static final float OTHER_HEAL = 8.0F;

    public LifeStaffItem() {
        super(300);
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

        boolean changed = heal(serverLevel, serverPlayer, stack, hand, serverPlayer, SELF_HEAL, SELF_DURABILITY_COST);
        return changed ? InteractionResultHolder.sidedSuccess(stack, false) : InteractionResultHolder.fail(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity creature, InteractionHand hand) {
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = creature.level();
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResult.FAIL;
        }

        boolean changed = heal(serverLevel, serverPlayer, stack, hand, creature, OTHER_HEAL, OTHER_DURABILITY_COST);
        return changed ? InteractionResult.sidedSuccess(false) : InteractionResult.FAIL;
    }

    /**
     * Heals {@code target} and clears its harmful effects. Returns false (and spends
     * nothing) when the target is already healthy and carries no harmful effects.
     */
    private static boolean heal(ServerLevel level, ServerPlayer caster, ItemStack stack, InteractionHand hand,
                                LivingEntity target, float amount, int durabilityCost) {
        boolean healed = false;
        if (target.getHealth() < target.getMaxHealth()) {
            target.heal(amount);
            healed = true;
        }
        boolean cured = clearHarmfulEffects(target);
        if (!healed && !cured) {
            caster.displayClientMessage(Component.translatable("message.elementalstaves.life.healthy"), true);
            return false;
        }

        level.sendParticles(ParticleTypes.HEART,
                target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(),
                8 + (healed ? 4 : 0), 0.35D, 0.35D, 0.35D, 0.0D);
        level.playSound(null, target.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 1.1F);
        completeCast(caster, stack, hand, COOLDOWN_TICKS, durabilityCost);
        return true;
    }

    private static boolean clearHarmfulEffects(LivingEntity target) {
        boolean cleared = false;
        for (MobEffectInstance instance : new ArrayList<>(target.getActiveEffects())) {
            if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                target.removeEffect(instance.getEffect());
                cleared = true;
            }
        }
        return cleared;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, net.minecraft.world.item.TooltipFlag tooltipFlag) {
        addDescription(tooltip, "tooltip.elementalstaves.life_staff.1", "tooltip.elementalstaves.life_staff.2");
    }
}
