package com.vvdd7232.elementalstaves.compat.create;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.vvdd7232.elementalstaves.mechanical.CoalEngineBlock;
import com.vvdd7232.elementalstaves.mechanical.EngineAccess;
import com.vvdd7232.elementalstaves.mechanical.MechanicalPlatform;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

public final class CreateEngineBlock extends DirectionalKineticBlock implements IBE<CreateEngineEntity> {
    public CreateEngineBlock(Properties p) {
        super(p.pushReaction(PushReaction.BLOCK));
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(CoalEngineBlock.LIT, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b); b.add(CoalEngineBlock.LIT);
    }
    @Override public Direction.Axis getRotationAxis(BlockState s) { return s.getValue(FACING).getAxis(); }
    @Override public boolean hasShaftTowards(LevelReader l, BlockPos p, BlockState s, Direction face) { return s.getValue(FACING) == face; }
    @Override protected boolean areStatesKineticallyEquivalent(BlockState old, BlockState next) {
        return old.getBlock() == next.getBlock() && old.getValue(FACING) == next.getValue(FACING);
    }
    @Override public Class<CreateEngineEntity> getBlockEntityClass() { return CreateEngineEntity.class; }
    @Override public BlockEntityType<? extends CreateEngineEntity> getBlockEntityType() { return MechanicalPlatform.engineType(); }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState s, Level l, BlockPos p,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!stack.is(Items.COAL) && !stack.is(Items.CHARCOAL)) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (player.isSpectator() || !player.mayBuild() || !l.mayInteract(player, p)) return ItemInteractionResult.FAIL;
        if (!l.isClientSide && l.getBlockEntity(p) instanceof EngineAccess engine) engine.insert(player, stack);
        return ItemInteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState s, Level l, BlockPos p, Player player, BlockHitResult hit) {
        if (player.isSpectator() || !player.mayBuild() || !l.mayInteract(player, p)) return InteractionResult.FAIL;
        if (!l.isClientSide && l.getBlockEntity(p) instanceof EngineAccess engine) engine.interactEmpty(player);
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override public void onRemove(BlockState s, Level l, BlockPos p, BlockState next, boolean moving) {
        if (!moving && !s.is(next.getBlock()) && l.getBlockEntity(p) instanceof EngineAccess engine) engine.dropFuel();
        super.onRemove(s, l, p, next, moving);
    }
    @Override public void animateTick(BlockState s, Level l, BlockPos p, RandomSource r) {
        if (s.getValue(CoalEngineBlock.LIT)) l.addParticle(ParticleTypes.SMOKE, p.getX()+0.5, p.getY()+1.05, p.getZ()+0.5, 0, 0.03, 0);
    }
}
