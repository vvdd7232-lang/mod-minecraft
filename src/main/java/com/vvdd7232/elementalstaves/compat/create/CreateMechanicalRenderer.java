package com.vvdd7232.elementalstaves.compat.create;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.vvdd7232.elementalstaves.client.MechanicalRenderer;
import com.vvdd7232.elementalstaves.mechanical.CoalEngineBlock;
import com.vvdd7232.elementalstaves.mechanical.DriveShaftBlock;
import com.vvdd7232.elementalstaves.mechanical.MechanicalPlatform;
import com.vvdd7232.elementalstaves.mechanical.RotatingBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Ordinary BER fallback remains visible with Flywheel enabled: no visual is registered for these types. */
public final class CreateMechanicalRenderer<T extends KineticBlockEntity> implements BlockEntityRenderer<T> {
    private final MechanicalRenderer<RotatingBlockEntity> model;
    public CreateMechanicalRenderer(BlockEntityRendererProvider.Context context) { model = new MechanicalRenderer<>(context); }
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MechanicalPlatform.<CreateEngineEntity>engineType(), CreateMechanicalRenderer::new);
        event.registerBlockEntityRenderer(MechanicalPlatform.<CreateShaftEntity>shaftType(), CreateMechanicalRenderer::new);
    }
    @Override public void render(T be, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        boolean engine = be instanceof CreateEngineEntity;
        Direction facing = engine ? be.getBlockState().getValue(CoalEngineBlock.FACING)
                : Direction.fromAxisAndDirection(be.getBlockState().getValue(DriveShaftBlock.AXIS), Direction.AxisDirection.POSITIVE);
        float angle = (float)Math.toDegrees(KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), facing.getAxis()));
        model.renderRotor(facing, engine, angle, pose, buffers, light, overlay);
    }
}
