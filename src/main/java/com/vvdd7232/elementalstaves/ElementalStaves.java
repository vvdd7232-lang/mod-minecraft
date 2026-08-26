package com.vvdd7232.elementalstaves;

import com.mojang.logging.LogUtils;
import com.vvdd7232.elementalstaves.item.EarthStaffItem;
import com.vvdd7232.elementalstaves.item.FireStaffItem;
import com.vvdd7232.elementalstaves.item.LightningStaffItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

/** NeoForge entry point and item registry for Elemental Staves. */
@Mod(ElementalStaves.MOD_ID)
public final class ElementalStaves {
    public static final String MOD_ID = "elementalstaves";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredItem<FireStaffItem> FIRE_STAFF = ITEMS.register("fire_staff", FireStaffItem::new);
    public static final DeferredItem<LightningStaffItem> LIGHTNING_STAFF = ITEMS.register("lightning_staff", LightningStaffItem::new);
    public static final DeferredItem<EarthStaffItem> EARTH_STAFF = ITEMS.register("earth_staff", EarthStaffItem::new);

    public ElementalStaves(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        modEventBus.addListener(this::addCreativeItems);
        LOGGER.info("Elemental Staves loaded for NeoForge: fire, lightning and earth are ready.");
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(FIRE_STAFF);
            event.accept(LIGHTNING_STAFF);
            event.accept(EARTH_STAFF);
        }
    }
}
