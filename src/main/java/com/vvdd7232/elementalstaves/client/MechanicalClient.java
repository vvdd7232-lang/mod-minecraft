package com.vvdd7232.elementalstaves.client;

import com.vvdd7232.elementalstaves.ElementalStaves;
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
        event.registerBlockEntityRenderer(ElementalStaves.COAL_ENGINE_ENTITY.get(), MechanicalRenderer::new);
        event.registerBlockEntityRenderer(ElementalStaves.DRIVE_SHAFT_ENTITY.get(), MechanicalRenderer::new);
    }
}
