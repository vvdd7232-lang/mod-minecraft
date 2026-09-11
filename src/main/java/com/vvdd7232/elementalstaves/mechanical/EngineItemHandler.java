package com.vvdd7232.elementalstaves.mechanical;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;

/** Funnels, chutes, hoppers and other item handlers see only queued fuel, never burning fuel. */
public final class EngineItemHandler implements IItemHandler {
    private final EngineAccess engine;
    public EngineItemHandler(EngineAccess engine) { this.engine = engine; }
    private void check(int slot) { if (slot != 0) throw new IndexOutOfBoundsException(slot); }
    @Override public int getSlots() { return 1; }
    @Override public int getSlotLimit(int slot) { check(slot); return 64; }
    @Override public boolean isItemValid(int slot, ItemStack stack) {
        check(slot); return stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
    }
    @Override public ItemStack getStackInSlot(int slot) {
        check(slot);
        EngineFuel fuel = engine.engineFuel();
        return fuel.queued() == 0 ? ItemStack.EMPTY : new ItemStack(fuel.charcoal() ? Items.CHARCOAL : Items.COAL, fuel.queued());
    }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!isItemValid(slot, stack)) return stack;
        EngineFuel fuel = engine.engineFuel();
        boolean charcoal = stack.is(Items.CHARCOAL);
        if (fuel.queued() > 0 && charcoal != fuel.charcoal()) return stack;
        int count = Math.min(stack.getCount(), 64 - fuel.queued());
        if (count == 0) return stack;
        if (!simulate) { fuel.insert(charcoal, count); engine.engineEntity().setChanged(); }
        return stack.copyWithCount(stack.getCount() - count);
    }
    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        check(slot);
        EngineFuel fuel = engine.engineFuel();
        int count = Math.clamp(amount, 0, fuel.queued());
        if (count == 0) return ItemStack.EMPTY;
        ItemStack result = new ItemStack(fuel.charcoal() ? Items.CHARCOAL : Items.COAL, count);
        if (!simulate) { fuel.extract(count); engine.engineEntity().setChanged(); }
        return result;
    }
}
