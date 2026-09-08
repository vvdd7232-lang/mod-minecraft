package com.vvdd7232.elementalstaves;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Tags used to define the harvest level of the Elemental tool tier. */
public final class ElementalTags {
    private ElementalTags() {
    }

    public static final TagKey<Block> INCORRECT_FOR_ELEMENTAL_TOOL = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(ElementalStaves.MOD_ID, "incorrect_for_elemental_tool")
    );
}
