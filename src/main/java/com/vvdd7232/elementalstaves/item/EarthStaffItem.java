package com.vvdd7232.elementalstaves.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Moves one ordinary block at a time. A source is stored on the ItemStack, so each
 * staff may hold its own selection and players cannot accidentally share a target.
 */
public final class EarthStaffItem extends BaseStaffItem {
    private static final String SOURCE_POS_KEY = "EarthSourcePos";
    private static final String SOURCE_DIMENSION_KEY = "EarthSourceDimension";
    private static final double PLAYER_RANGE = 24.0D;
    private static final double MAX_MOVE_DISTANCE = 16.0D;
    private static final int COOLDOWN_TICKS = 12;
    private static final int DURABILITY_COST = 3;

    /* Blocks that should never be movable even when they do not expose a block entity. */
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
    public ActionResult useOnBlock(net.minecraft.item.ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null) {
            return ActionResult.PASS;
        }

        World world = context.getWorld();
        ItemStack stack = context.getStack();
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        BlockPos clickedPos = context.getBlockPos();
        SourceSelection selected = readSelection(stack, world);
        // Sneaking is an explicit, safe way to replace a previous selection.
        if (player.isSneaking() || selected == null) {
            return selectSource((ServerWorld) world, player, stack, clickedPos);
        }

        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return ActionResult.FAIL;
        }
        return moveSelectedBlock((ServerWorld) world, player, stack, context.getHand(), selected, clickedPos.offset(context.getSide()));
    }

    private ActionResult selectSource(ServerWorld world, PlayerEntity player, ItemStack stack, BlockPos source) {
        BlockState state = world.getBlockState(source);
        if (!world.canPlayerModifyAt(player, source)) {
            player.sendMessage(Text.translatable("message.elementalstaves.common.protected"), true);
            return ActionResult.FAIL;
        }
        if (!isMovable(world, source, state)) {
            player.sendMessage(Text.translatable("message.elementalstaves.earth.not_movable"), true);
            return ActionResult.FAIL;
        }

        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putLong(SOURCE_POS_KEY, source.asLong());
        nbt.putString(SOURCE_DIMENSION_KEY, world.getRegistryKey().getValue().toString());

        Vec3d center = Vec3d.ofCenter(source);
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, center.x, center.y, center.z, 12, 0.35D, 0.35D, 0.35D, 0.02D);
        world.playSound(null, source, state.getSoundGroup().getHitSound(), SoundCategory.BLOCKS, 0.9F, 1.15F);
        player.sendMessage(Text.translatable("message.elementalstaves.earth.selected"), true);
        return ActionResult.SUCCESS;
    }

    private ActionResult moveSelectedBlock(ServerWorld world, PlayerEntity player, ItemStack stack, Hand hand, SourceSelection selection, BlockPos destination) {
        BlockPos source = selection.pos();
        BlockState sourceState = world.getBlockState(source);

        if (!withinRange(player, source) || !withinRange(player, destination)
                || source.getSquaredDistance(destination) > MAX_MOVE_DISTANCE * MAX_MOVE_DISTANCE) {
            player.sendMessage(Text.translatable("message.elementalstaves.earth.too_far"), true);
            return ActionResult.FAIL;
        }
        if (!world.canPlayerModifyAt(player, source) || !world.canPlayerModifyAt(player, destination)) {
            player.sendMessage(Text.translatable("message.elementalstaves.common.protected"), true);
            return ActionResult.FAIL;
        }
        if (!world.isInBuildLimit(destination) || !world.getWorldBorder().contains(destination)) {
            player.sendMessage(Text.translatable("message.elementalstaves.earth.invalid_destination"), true);
            return ActionResult.FAIL;
        }
        if (!isMovable(world, source, sourceState)) {
            clearSelection(stack);
            player.sendMessage(Text.translatable("message.elementalstaves.earth.source_changed"), true);
            return ActionResult.FAIL;
        }
        if (!world.getBlockState(destination).isAir() || !sourceState.canPlaceAt(world, destination)) {
            player.sendMessage(Text.translatable("message.elementalstaves.earth.destination_blocked"), true);
            return ActionResult.FAIL;
        }

        // Place first. If placement is rejected, the source remains untouched.
        if (!world.setBlockState(destination, sourceState, Block.NOTIFY_ALL)) {
            player.sendMessage(Text.translatable("message.elementalstaves.earth.destination_blocked"), true);
            return ActionResult.FAIL;
        }
        world.setBlockState(source, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        clearSelection(stack);

        Vec3d sourceCenter = Vec3d.ofCenter(source);
        Vec3d destinationCenter = Vec3d.ofCenter(destination);
        world.spawnParticles(ParticleTypes.CLOUD, sourceCenter.x, sourceCenter.y, sourceCenter.z, 10, 0.25D, 0.25D, 0.25D, 0.02D);
        world.spawnParticles(ParticleTypes.POOF, destinationCenter.x, destinationCenter.y, destinationCenter.z, 14, 0.25D, 0.25D, 0.25D, 0.03D);
        world.playSound(null, destination, sourceState.getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS, 1.0F, 0.95F);
        completeCast(player, stack, hand, COOLDOWN_TICKS, DURABILITY_COST);
        player.sendMessage(Text.translatable("message.elementalstaves.earth.moved"), true);
        return ActionResult.SUCCESS;
    }

    private static boolean withinRange(PlayerEntity player, BlockPos pos) {
        return player.squaredDistanceTo(Vec3d.ofCenter(pos)) <= PLAYER_RANGE * PLAYER_RANGE;
    }

    private static boolean isMovable(World world, BlockPos pos, BlockState state) {
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && !state.hasBlockEntity()
                && state.getHardness(world, pos) >= 0.0F
                && !PROTECTED_BLOCKS.contains(state.getBlock());
    }

    private static SourceSelection readSelection(ItemStack stack, World world) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(SOURCE_POS_KEY) || !nbt.contains(SOURCE_DIMENSION_KEY)) {
            return null;
        }
        if (!world.getRegistryKey().getValue().toString().equals(nbt.getString(SOURCE_DIMENSION_KEY))) {
            // A position in another dimension is intentionally never used here.
            clearSelection(stack);
            return null;
        }
        return new SourceSelection(BlockPos.fromLong(nbt.getLong(SOURCE_POS_KEY)));
    }

    private static void clearSelection(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) {
            return;
        }
        nbt.remove(SOURCE_POS_KEY);
        nbt.remove(SOURCE_DIMENSION_KEY);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        addDescription(tooltip, "tooltip.elementalstaves.earth_staff.1", "tooltip.elementalstaves.earth_staff.2");
    }

    private record SourceSelection(BlockPos pos) {
    }
}
