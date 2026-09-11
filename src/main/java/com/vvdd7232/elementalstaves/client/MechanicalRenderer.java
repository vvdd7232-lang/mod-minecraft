package com.vvdd7232.elementalstaves.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.vvdd7232.elementalstaves.ElementalStaves;
import com.vvdd7232.elementalstaves.mechanical.CoalEngineBlock;
import com.vvdd7232.elementalstaves.mechanical.CoalEngineBlockEntity;
import com.vvdd7232.elementalstaves.mechanical.DriveShaftBlock;
import com.vvdd7232.elementalstaves.mechanical.RotatingBlockEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** Only the rotor moves; casing and bearings use ordinary baked block models. */
public final class MechanicalRenderer<T extends RotatingBlockEntity> implements BlockEntityRenderer<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(ElementalStaves.MOD_ID, "mechanical_rotor"), "main");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ElementalStaves.MOD_ID, "textures/entity/mechanical_rotor.png");
    private final ModelPart shaft;
    private final ModelPart wheel;

    public MechanicalRenderer(BlockEntityRendererProvider.Context context) {
        ModelPart model = context.bakeLayer(LAYER);
        shaft = model.getChild("shaft");
        wheel = model.getChild("wheel");
    }

    public static LayerDefinition layer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("shaft", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2, -2, -8, 4, 4, 16)
                .texOffs(40, 0).addBox(-3, -3, -2, 6, 6, 4), PartPose.ZERO);
        mesh.getRoot().addOrReplaceChild("wheel", CubeListBuilder.create()
                .texOffs(0, 20).addBox(-5, -5, 8, 10, 10, 2)
                .texOffs(40, 12).addBox(-2, -2, 6, 4, 4, 5)
                .texOffs(48, 24).addBox(2, -1, 10, 2, 2, 2), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override public void render(T entity, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        boolean engine = entity instanceof CoalEngineBlockEntity;
        Direction facing = engine ? entity.getBlockState().getValue(CoalEngineBlock.FACING)
                : Direction.fromAxisAndDirection(entity.getBlockState().getValue(DriveShaftBlock.AXIS), Direction.AxisDirection.POSITIVE);
        switch (facing) {
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(-90));
            case UP -> pose.mulPose(Axis.XP.rotationDegrees(-90));
            case DOWN -> pose.mulPose(Axis.XP.rotationDegrees(90));
            default -> { }
        }
        float sign = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1;
        pose.mulPose(Axis.ZP.rotationDegrees(entity.angle(partialTick) * sign));
        (engine ? wheel : shaft).render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        pose.popPose();
    }
}
