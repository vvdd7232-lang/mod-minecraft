package com.vvdd7232.elementalstaves.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/**
 * Moves one ordinary block at a time. A source is stored in the item's custom
 * data component, so every individual staff keeps an isolated selection.
 */
public final class EarthStaffItem extends BaseStaffItem {
    private static final String SOURCE_POS_KEY = "EarthSourcePos";
    private static final String SOURCE_DIMENSION_KEY = "EarthSourceDimension";
    private static final double PLAYER_RANGE = 24.0D;
    private static final double MAX_MOVE_DISTANCE = 16.0D;
    private static final int COOLDOWN_TICKS = 12;
    private static final int DURABILITY_COST = 3;

    /* Blocks that must never be movable even when they do not expose a block entity. */
    private static final Set<Block> PROTECTED_BLOCKS = Set.of(
            Blocks.BEDROCK,
            Blocks.BARRIER,
            Blocks.END_PORTAL,
            Blocks.END_GATEWAY,
            Blocks.END_PORTAL_FRAME,
            Blocks.NETHER_PORTAL,
            Blocks.COMMAND_BLOCK,
            Blocks.CHAIN_COMMAND_BLOCK,
            Blocks.REPEATING_COMMAND_BLOCK,
            Blocks.STRUCTURE_BLOCK,
            Blocks.JIGSAW,
            Blocks.LIGHT,
            Blocks.REINFORCED_DEEPSLATE
    );

    public EarthStaffItem() {
        super(512);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        BlockPos clickedPos = context.getClickedPos();
        SourceSelection selected = readSelection(stack, serverLevel);
        // Sneak-use is an explicit and safe way to replace an old source selection.
        if (player.isShiftKeyDown() || selected == null) {
            return selectSource(serverLevel, serverPlayer, stack, clickedPos);
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResult.FAIL;
        }
        return moveSelectedBlock(serverLevel, serverPlayer, stack, context.getHand(), selected, clickedPos.relative(context.getClickedFace()));
    }

    private InteractionResult selectSource(ServerLevel level, ServerPlayer player, ItemStack stack, BlockPos source) {
        BlockState state = level.getBlockState(source);
        if (!level.mayInteract(player, source)) {
            player.displayClientMessage(Component.translatable("message.elementalstaves.common.protected"), true);
            return InteractionResult.FAIL;
        }
        if (!isMovable(level, source, state)) {
            player.displayClientMessage(Component.translatable("message.elementalstaves.earth.not_movable"), true);
            return InteractionResult.FAIL;
        }

        CompoundTag selectionData = getCustomDataCopy(stack);
        selectionData.putLong(SOURCE_POS_KEY, source.asLong());
        selectionData.putString(SOURCE_DIMENSION_KEY, level.dimension().location().toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(selectionData));

        Vec3 center = source.getCenter();
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.x, center.y, center.z, 12, 0.35D, 0.35D, 0.35D, 0.02D);
        level.playSound(null, source, state.getSoundType().getHitSound(), SoundSource.BLOCKS, 0.9F, 1.15F);
        player.displayClientMessage(Component.translatable("message.elementalstaves.earth.selected"), true);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult moveSelectedBlock(ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand, SourceSelection selection, BlockPos destination) {
        BlockPos source = selection.pos();
        BlockState sourceState = level.getBlockState(source);

        if (!withinRange(player, source) || !withinRange(player, destination)
                || source.distSqr(destination) > MAX_MOVE_DISTANCE * MAX_MOVE_DISTANCE) {
            player.displayClientMessage(Component.translatable("message.elementalstaves.earth.too_far"), true);
            return InteractionResult.FAIL;
        }
        if (!level.mayInteract(player, source) || !level.mayInteract(player, destination)) {
            player.displayClientMessage(Component.translatable("message.elementalstaves.common.protected"), true);
            return InteractionResult.FAIL;
        }
        if (!level.isInWorldBounds(destination) || !level.getWorldBorder().isWithinBounds(destination)) {
            player.displayClientMessage(Component.translatable("message.elementalstaves.earth.invalid_destination"), true);
            return InteractionResult.FAIL;
        }
        if (!isMovable(level, source, sourceState)) {
            clearSelection(stack);
            player.displayClientMessage(Component.translatable("message.elementalstaves.earth.source_changed"), true);
            return InteractionResult.FAIL;
        }
        if (!level.getBlockState(destination).isAir() || !sourceState.canSurvive(level, destination)) {
            player.displayClientMessage(Component.translatable("message.elementalstaves.earth.destination_blocked"), true);
            return InteractionResult.FAIL;
        }

        // Place first. If placement is rejected, the source block stays untouched.
        if (!level.setBlock(destination, sourceState, Block.UPDATE_ALL)) {
            player.displayClientMessage(Component.translatable("message.elementalstaves.earth.destination_blocked"), true);
            return InteractionResult.FAIL;
        }
        level.setBlock(source, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        clearSelection(stack);

        Vec3 sourceCenter = source.getCenter();
        Vec3 destinationCenter = destination.getCenter();
        level.sendParticles(ParticleTypes.CLOUD, sourceCenter.x, sourceCenter.y, sourceCenter.z, 10, 0.25D, 0.25D, 0.25D, 0.02D);
        level.sendParticles(ParticleTypes.POOF, destinationCenter.x, destinationCenter.y, destinationCenter.z, 14, 0.25D, 0.25D, 0.25D, 0.03D);
        level.playSound(null, destination, sourceState.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.95F);
        completeCast(player, stack, hand, COOLDOWN_TICKS, DURABILITY_COST);
        player.displayClientMessage(Component.translatable("message.elementalstaves.earth.moved"), true);
        return InteractionResult.SUCCESS;
    }

    private static boolean withinRange(Player player, BlockPos pos) {
        return player.distanceToSqr(pos.getCenter()) <= PLAYER_RANGE * PLAYER_RANGE;
    }

    private static boolean isMovable(Level level, BlockPos pos, BlockState state) {
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && !state.hasBlockEntity()
                && state.getDestroySpeed(level, pos) >= 0.0F
                && !PROTECTED_BLOCKS.contains(state.getBlock());
    }

    private static SourceSelection readSelection(ItemStack stack, Level level) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(SOURCE_POS_KEY) || !tag.contains(SOURCE_DIMENSION_KEY)) {
            return null;
        }
        if (!level.dimension().location().toString().equals(tag.getString(SOURCE_DIMENSION_KEY))) {
            // Never resolve a saved position in a different dimension.
            clearSelection(stack);
            return null;
        }
        return new SourceSelection(BlockPos.of(tag.getLong(SOURCE_POS_KEY)));
    }

    private static CompoundTag getCustomDataCopy(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    private static void clearSelection(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return;
        }
        CompoundTag tag = data.copyTag();
        tag.remove(SOURCE_POS_KEY);
        tag.remove(SOURCE_DIMENSION_KEY);
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, net.minecraft.world.item.TooltipFlag tooltipFlag) {
        addDescription(tooltip, "tooltip.elementalstaves.earth_staff.1", "tooltip.elementalstaves.earth_staff.2");
    }

    private record SourceSelection(BlockPos pos) {
    }
}
