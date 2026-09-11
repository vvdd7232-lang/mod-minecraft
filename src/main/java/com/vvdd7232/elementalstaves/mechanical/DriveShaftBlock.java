package com.vvdd7232.elementalstaves.mechanical;

import com.mojang.serialization.MapCodec;
import com.vvdd7232.elementalstaves.ElementalStaves;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class DriveShaftBlock extends BaseEntityBlock {
    public static final MapCodec<DriveShaftBlock> CODEC = simpleCodec(DriveShaftBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty ROTATING = BooleanProperty.create("rotating");
    private static final VoxelShape X = Block.box(0, 5, 5, 16, 11, 11);
    private static final VoxelShape Y = Block.box(5, 0, 5, 11, 16, 11);
    private static final VoxelShape Z = Block.box(5, 5, 0, 11, 11, 16);

    public DriveShaftBlock(Properties properties) {
        super(properties.pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Y).setValue(ROTATING, false));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(AXIS, ROTATING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(AXIS, c.getClickedFace().getAxis()); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return switch (s.getValue(AXIS)) { case X -> X; case Y -> Y; case Z -> Z; };
    }
    @Override protected BlockState rotate(BlockState s, Rotation r) {
        if (r == Rotation.CLOCKWISE_90 || r == Rotation.COUNTERCLOCKWISE_90) {
            if (s.getValue(AXIS) == Direction.Axis.X) return s.setValue(AXIS, Direction.Axis.Z);
            if (s.getValue(AXIS) == Direction.Axis.Z) return s.setValue(AXIS, Direction.Axis.X);
        }
        return s;
    }
    @Override protected BlockState mirror(BlockState s, Mirror m) { return s; }
    @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new DriveShaftBlockEntity(p, s); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return createTickerHelper(t, ElementalStaves.DRIVE_SHAFT_ENTITY.get(), DriveShaftBlockEntity::tick);
    }
}
