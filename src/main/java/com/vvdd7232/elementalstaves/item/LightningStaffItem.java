package com.vvdd7232.elementalstaves.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

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
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        if (world.isClient) {
            return TypedActionResult.success(stack);
        }

        HitResult hit = user.raycast(RANGE, 1.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            user.sendMessage(Text.translatable("message.elementalstaves.lightning.no_target"), true);
            return TypedActionResult.fail(stack);
        }

        BlockPos target = ((BlockHitResult) hit).getBlockPos();
        if (!world.canPlayerModifyAt(user, target)) {
            user.sendMessage(Text.translatable("message.elementalstaves.common.protected"), true);
            return TypedActionResult.fail(stack);
        }

        LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(world);
        if (lightning == null) {
            return TypedActionResult.fail(stack);
        }

        lightning.setPosition(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        if (user instanceof ServerPlayerEntity serverPlayer) {
            lightning.setChanneler(serverPlayer);
        }
        world.spawnEntity(lightning);

        world.playSound(null, target, SoundEvents.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 0.75F, 1.0F);
        ((ServerWorld) world).spawnParticles(ParticleTypes.ELECTRIC_SPARK, target.getX() + 0.5D, target.getY() + 1.0D, target.getZ() + 0.5D, 18, 0.35D, 0.35D, 0.35D, 0.12D);
        completeCast(user, stack, hand, COOLDOWN_TICKS, DURABILITY_COST);
        return TypedActionResult.success(stack, false);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        addDescription(tooltip, "tooltip.elementalstaves.lightning_staff.1", "tooltip.elementalstaves.lightning_staff.2");
    }
}
