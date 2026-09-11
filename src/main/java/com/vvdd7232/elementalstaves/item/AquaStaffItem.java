package com.vvdd7232.elementalstaves.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * The Aqua Staff douses fire and burning creatures around the targeted block and,
 * when the cell next to the clicked face is empty, pours an infinite water source
 * into it. All checks run server-side only.
 */
public final class AquaStaffItem extends BaseStaffItem {
    private static final double RANGE = 12.0D;
    private static final int FIRE_RADIUS = 4;
    private static final int COOLDOWN_TICKS = 30;
    private static final int DURABILITY_COST = 2;

    public AquaStaffItem() {
        super(320);
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) return castSpell(level, player, hand, StaffMagic.Spell.FROST);
        return net.minecraft.world.InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) return use(context.getLevel(), player, context.getHand()).getResult();
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResult.FAIL;
        }

        BlockPos clickedPos = context.getClickedPos();
        BlockPos waterCell = clickedPos.relative(context.getClickedFace());
        if (!withinRange(player, clickedPos) || !withinRange(player, waterCell)) {
            player.displayClientMessage(Component.translatable("message.elementalstaves.aqua.too_far"), true);
            return InteractionResult.FAIL;
        }
        if (!player.mayBuild() || !serverLevel.mayInteract(serverPlayer, clickedPos) || !serverLevel.mayInteract(serverPlayer, waterCell)) {
            player.displayClientMessage(Component.translatable("message.elementalstaves.common.protected"), true);
            return InteractionResult.FAIL;
        }

        int fires = douseFires(serverLevel, serverPlayer, clickedPos);
        boolean splashed = douseCreatures(serverLevel, clickedPos);
        boolean poured = pourWater(serverLevel, waterCell);

        if (fires == 0 && !splashed && !poured) {
            player.displayClientMessage(Component.translatable("message.elementalstaves.aqua.no_effect"), true);
            return InteractionResult.FAIL;
        }

        if (poured) {
            serverLevel.playSound(null, waterCell, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else if (fires > 0 || splashed) {
            serverLevel.playSound(null, clickedPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.9F, 1.0F);
        }
        completeCast(serverPlayer, stack, context.getHand(), COOLDOWN_TICKS, DURABILITY_COST);
        return InteractionResult.SUCCESS;
    }

    /** Replaces every fire block in a cube around {@code center} with air. Returns how many were put out. */
    private static int douseFires(ServerLevel level, ServerPlayer player, BlockPos center) {
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-FIRE_RADIUS, -FIRE_RADIUS, -FIRE_RADIUS),
                center.offset(FIRE_RADIUS, FIRE_RADIUS, FIRE_RADIUS))) {
            if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos) || !player.mayBuild()
                    || !level.getWorldBorder().isWithinBounds(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                count++;
            }
        }
        if (count > 0) {
            level.sendParticles(ParticleTypes.SMOKE, center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D,
                    20 + count, 1.5D, 1.5D, 1.5D, 0.02D);
        }
        return count;
    }

    /** Clears fire from every burning creature in the same cube. */
    private static boolean douseCreatures(ServerLevel level, BlockPos center) {
        boolean any = false;
        List<LivingEntity> creatures = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center).inflate(FIRE_RADIUS));
        for (LivingEntity creature : creatures) {
            if (creature.isOnFire()) {
                creature.clearFire();
                any = true;
            }
        }
        if (any) {
            level.sendParticles(ParticleTypes.SPLASH, center.getX() + 0.5D, center.getY() + 1.0D, center.getZ() + 0.5D,
                    12, 0.8D, 0.5D, 0.8D, 0.05D);
        }
        return any;
    }

    /** Places a still water source into {@code waterCell} when it can be replaced. */
    private static boolean pourWater(ServerLevel level, BlockPos waterCell) {
        if (level.dimensionType().ultraWarm() || !level.isInWorldBounds(waterCell)
                || !level.getWorldBorder().isWithinBounds(waterCell)) return false;
        BlockState state = level.getBlockState(waterCell);
        if (state.canBeReplaced() && state.getFluidState().isEmpty()
                && level.setBlock(waterCell, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL)) {
            level.sendParticles(ParticleTypes.BUBBLE_POP, waterCell.getX() + 0.5D, waterCell.getY() + 0.5D,
                    waterCell.getZ() + 0.5D, 14, 0.3D, 0.3D, 0.3D, 0.02D);
            return true;
        }
        return false;
    }

    private static boolean withinRange(Player player, BlockPos pos) {
        return player.distanceToSqr(pos.getCenter()) <= RANGE * RANGE;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, net.minecraft.world.item.TooltipFlag tooltipFlag) {
        addDescription(tooltip, "tooltip.elementalstaves.aqua_staff.1", "tooltip.elementalstaves.aqua_staff.2");
    }
}
