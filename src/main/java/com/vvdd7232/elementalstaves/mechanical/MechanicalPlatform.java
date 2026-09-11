package com.vvdd7232.elementalstaves.mechanical;

import com.vvdd7232.elementalstaves.ElementalStaves;
import com.vvdd7232.elementalstaves.compat.create.CreateIntegration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/** Optional boundary: no Create type appears in a public signature or field here. */
public final class MechanicalPlatform {
    private MechanicalPlatform() {}
    public static boolean hasCreate() { return ModList.get().isLoaded("create"); }
    public static Block engine(BlockBehaviour.Properties p) {
        return hasCreate() ? CreateIntegration.engine(p) : new CoalEngineBlock(p);
    }
    public static Block shaft(BlockBehaviour.Properties p) {
        return hasCreate() ? CreateIntegration.shaft(p) : new DriveShaftBlock(p);
    }
    public static BlockEntityType<?> buildEngineType() {
        return hasCreate() ? CreateIntegration.buildEngineType()
                : BlockEntityType.Builder.of(CoalEngineBlockEntity::new, ElementalStaves.COAL_ENGINE.get()).build(null);
    }
    public static BlockEntityType<?> buildShaftType() {
        return hasCreate() ? CreateIntegration.buildShaftType()
                : BlockEntityType.Builder.of(DriveShaftBlockEntity::new, ElementalStaves.DRIVE_SHAFT.get()).build(null);
    }
    // Backend-specific consumers request their concrete type only after selecting the backend.
    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> BlockEntityType<T> engineType() { return (BlockEntityType<T>) ElementalStaves.COAL_ENGINE_ENTITY.get(); }
    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> BlockEntityType<T> shaftType() { return (BlockEntityType<T>) ElementalStaves.DRIVE_SHAFT_ENTITY.get(); }
    public static void init(IEventBus bus) {
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) -> {
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    MechanicalPlatform.<BlockEntity>engineType(),
                    (be, side) -> be instanceof EngineAccess engine ? new EngineItemHandler(engine) : null);
        });
        bus.addListener((FMLCommonSetupEvent event) -> {
            if (hasCreate()) event.enqueueWork(CreateIntegration::setup);
        });
    }
}
