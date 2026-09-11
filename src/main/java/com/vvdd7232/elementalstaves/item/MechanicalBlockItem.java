package com.vvdd7232.elementalstaves.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import java.util.List;

public final class MechanicalBlockItem extends BlockItem {
    private final String tooltip;
    public MechanicalBlockItem(Block block, Properties properties, String name) {
        super(block, properties);
        tooltip = "tooltip.elementalstaves." + name;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable(tooltip + ".1").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(tooltip + ".2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
