package com.vvdd7232.elementalstaves.mechanical;

import com.mojang.serialization.MapCodec;
import com.vvdd7232.elementalstaves.ElementalStaves;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

public final class CoalEngineBlock extends BaseEntityBlock {
    public static final MapCodec<CoalEngineBlock> CODEC = simpleCodec(CoalEngineBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public CoalEngineBlock(Properties properties) {
        super(properties.pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, LIT); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) {
        return defaultBlockState().setValue(FACING, c.getNearestLookingDirection().getOpposite());
    }
    @Override protected BlockState rotate(BlockState s, Rotation r) { return s.setValue(FACING, r.rotate(s.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState s, Mirror m) { return rotate(s, m.getRotation(s.getValue(FACING))); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new CoalEngineBlockEntity(p, s); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return createTickerHelper(t, MechanicalPlatform.<CoalEngineBlockEntity>engineType(), CoalEngineBlockEntity::tick);
    }

    private boolean canUse(Level level, BlockPos pos, Player player) {
        return !player.isSpectator() && player.mayBuild() && level.mayInteract(player, pos);
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                        Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!stack.is(Items.COAL) && !stack.is(Items.CHARCOAL)) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (!canUse(level, pos, player)) return ItemInteractionResult.FAIL;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof EngineAccess engine) engine.insert(player, stack);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!canUse(level, pos, player)) return InteractionResult.FAIL;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof EngineAccess engine) engine.interactEmpty(player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof EngineAccess engine) engine.dropFuel();
        super.onRemove(state, level, pos, next, moving);
    }

    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) level.addParticle(ParticleTypes.SMOKE,
                pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0, 0.03, 0);
    }
}
