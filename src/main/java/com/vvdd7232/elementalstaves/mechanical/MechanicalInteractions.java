package com.vvdd7232.elementalstaves.mechanical;

import com.vvdd7232.elementalstaves.ElementalStaves;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Vanilla otherwise bypasses block use when sneaking with coal in hand. */
@EventBusSubscriber(modid = ElementalStaves.MOD_ID)
public final class MechanicalInteractions {
    private MechanicalInteractions() {}
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void allowFuelControls(PlayerInteractEvent.RightClickBlock event) {
        // Respect earlier protection handlers; actual insertion still checks player permissions.
        if (event.getUseBlock() == TriState.FALSE || !event.getEntity().isShiftKeyDown()) return;
        ItemStack held = event.getItemStack();
        if (!(held.isEmpty() || held.is(Items.COAL) || held.is(Items.CHARCOAL))) return;
        if (event.getLevel().getBlockState(event.getPos()).is(ElementalStaves.COAL_ENGINE.get()))
            event.setUseBlock(TriState.TRUE);
    }
}
