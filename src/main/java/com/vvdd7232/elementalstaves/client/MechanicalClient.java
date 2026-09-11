package com.vvdd7232.elementalstaves.client;

import com.vvdd7232.elementalstaves.ElementalStaves;
import com.vvdd7232.elementalstaves.mechanical.MechanicalPlatform;
import com.vvdd7232.elementalstaves.mechanical.CoalEngineBlockEntity;
import com.vvdd7232.elementalstaves.mechanical.DriveShaftBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Dist guard keeps all Minecraft client classes off a dedicated server. */
@EventBusSubscriber(modid = ElementalStaves.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MechanicalClient {
    private MechanicalClient() {}
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MechanicalRenderer.LAYER, MechanicalRenderer::layer);
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        if (MechanicalPlatform.hasCreate()) {
            com.vvdd7232.elementalstaves.compat.create.CreateMechanicalRenderer.register(event);
        } else {
            event.registerBlockEntityRenderer(MechanicalPlatform.<CoalEngineBlockEntity>engineType(), MechanicalRenderer::new);
            event.registerBlockEntityRenderer(MechanicalPlatform.<DriveShaftBlockEntity>shaftType(), MechanicalRenderer::new);
        }
    }
}
