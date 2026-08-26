package com.vvdd7232.elementalstaves;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

/** Balance values and repair rules shared by the Elemental tools. */
public final class ElementalMaterials {
    private ElementalMaterials() {
    }

    /**
     * A durable late-iron tier: it mines diamond-level blocks, but remains below diamond in speed and durability.
     */
    public static final Tier ELEMENTAL_TIER = new SimpleTier(
            ElementalTags.INCORRECT_FOR_ELEMENTAL_TOOL,
            900,
            7.0F,
            2.0F,
            18,
            () -> Ingredient.of(ElementalStaves.ELEMENTAL_INGOT.get())
    );
}
