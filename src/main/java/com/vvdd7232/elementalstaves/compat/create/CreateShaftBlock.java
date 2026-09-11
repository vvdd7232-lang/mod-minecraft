package com.vvdd7232.elementalstaves.compat.create;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.AbstractShaftBlock;
import com.vvdd7232.elementalstaves.mechanical.DriveShaftBlock;
import com.vvdd7232.elementalstaves.mechanical.MechanicalPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CreateShaftBlock extends AbstractShaftBlock {
    public CreateShaftBlock(Properties p) { super(p); registerDefaultState(defaultBlockState().setValue(DriveShaftBlock.ROTATING, false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b); b.add(DriveShaftBlock.ROTATING); // preserve saved standalone states
    }
    @Override public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() { return MechanicalPlatform.shaftType(); }
    @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return switch (s.getValue(AXIS)) {
            case X -> Block.box(0,5,5,16,11,11);
            case Y -> Block.box(5,0,5,11,16,11);
            case Z -> Block.box(5,5,0,11,11,16);
        };
    }
}
