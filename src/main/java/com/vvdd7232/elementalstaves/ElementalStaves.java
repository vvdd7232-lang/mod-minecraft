package com.vvdd7232.elementalstaves;

import com.vvdd7232.elementalstaves.item.EarthStaffItem;
import com.vvdd7232.elementalstaves.item.FireStaffItem;
import com.vvdd7232.elementalstaves.item.LightningStaffItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Entry point and registry for Elemental Staves. */
public final class ElementalStaves implements ModInitializer {
    public static final String MOD_ID = "elementalstaves";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Item FIRE_STAFF = register("fire_staff", new FireStaffItem());
    public static final Item LIGHTNING_STAFF = register("lightning_staff", new LightningStaffItem());
    public static final Item EARTH_STAFF = register("earth_staff", new EarthStaffItem());

    private static Item register(String path, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(MOD_ID, path), item);
    }

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(FIRE_STAFF);
            entries.add(LIGHTNING_STAFF);
            entries.add(EARTH_STAFF);
        });

        LOGGER.info("Elemental Staves loaded: fire, lightning and earth are ready.");
    }
}
