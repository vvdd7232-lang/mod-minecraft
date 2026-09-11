package com.vvdd7232.elementalstaves.compat.create;

import com.simibubi.create.api.stress.BlockStressValues;
import com.vvdd7232.elementalstaves.ElementalStaves;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Loaded only after detecting Create. Same registry IDs and fuel NBT as standalone mode. */
public final class CreateIntegration {
    private CreateIntegration() {}
    public static Block engine(BlockBehaviour.Properties p) { return new CreateEngineBlock(p); }
    public static Block shaft(BlockBehaviour.Properties p) { return new CreateShaftBlock(p); }
    public static BlockEntityType<?> buildEngineType() {
        return BlockEntityType.Builder.of(CreateEngineEntity::new, ElementalStaves.COAL_ENGINE.get()).build(null);
    }
    public static BlockEntityType<?> buildShaftType() {
        return BlockEntityType.Builder.of(CreateShaftEntity::new, ElementalStaves.DRIVE_SHAFT.get()).build(null);
    }
    public static void setup() {
        BlockStressValues.CAPACITIES.register(ElementalStaves.COAL_ENGINE.get(), () -> 64.0);
        BlockStressValues.IMPACTS.register(ElementalStaves.COAL_ENGINE.get(), () -> 0.0);
        BlockStressValues.IMPACTS.register(ElementalStaves.DRIVE_SHAFT.get(), () -> 0.0);
        BlockStressValues.RPM.register(ElementalStaves.COAL_ENGINE.get(), new BlockStressValues.GeneratedRpm(40, false));
    }
}
