package com.vvdd7232.elementalstaves.gametest;

import com.vvdd7232.elementalstaves.ElementalStaves;
import com.vvdd7232.elementalstaves.mechanical.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ElementalStaves.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MechanicalGameTests {
    @GameTest(template = "empty", timeoutTicks = 180)
    public static void driveAndRedstone(GameTestHelper h) {
        boolean create = MechanicalPlatform.hasCreate();
        h.assertTrue(create == Boolean.getBoolean("elementalstaves.testCreate"), "Wrong optional dependency profile");
        BlockPos engine = new BlockPos(1,1,1), shaft = new BlockPos(2,1,1), tail = new BlockPos(3,1,1);
        h.setBlock(engine, ElementalStaves.COAL_ENGINE.get().defaultBlockState().setValue(CoalEngineBlock.FACING, Direction.EAST));
        h.setBlock(shaft, ElementalStaves.DRIVE_SHAFT.get().defaultBlockState().setValue(DriveShaftBlock.AXIS, Direction.Axis.X));
        h.setBlock(tail, ElementalStaves.DRIVE_SHAFT.get().defaultBlockState().setValue(DriveShaftBlock.AXIS, Direction.Axis.X));
        EngineAccess access = (EngineAccess) h.getBlockEntity(engine);
        var inventory = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                h.absolutePos(engine), Direction.UP);
        h.assertTrue(inventory != null, "Fuel capability not registered");
        var coal = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL, 2);
        h.assertTrue(inventory.insertItem(0, coal, true).isEmpty(), "Simulated insertion rejected");
        h.assertTrue(access.engineFuel().queued() == 0, "Simulation mutated fuel");
        inventory.insertItem(0, coal, false);
        for (String recipe : new String[]{"crushing/elemental_ore", "crushing/deepslate_elemental_ore",
                "compacting/elemental_block", "compacting/raw_elemental_block", "mixing/elemental_ingot",
                "cutting/drive_shaft", "to_create_shaft", "from_create_shaft"}) {
            boolean present = h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation
                    .fromNamespaceAndPath(ElementalStaves.MOD_ID, "create/" + recipe)).isPresent();
            h.assertTrue(present == create, "Wrong conditional recipe state: " + recipe);
        }
        h.runAfterDelay(30, () -> {
            assertRotation(h, tail, true);
            h.assertTrue(access.engineFuel().queued() == 1, "Must consume exactly one coal at ignition");
            inventory.extractItem(0, 64, true);
            h.assertTrue(access.engineFuel().queued() == 1, "Simulated extraction mutated fuel");
            var saved = h.getBlockEntity(engine).saveWithFullMetadata(h.getLevel().registryAccess());
            h.assertTrue(saved.getInt("Fuel") == 1 && saved.getInt("BurnTicks") == access.engineFuel().remaining(),
                    "Fuel NBT format differs between backends");
            h.setBlock(new BlockPos(1,1,2), Blocks.REDSTONE_BLOCK);
        });
        h.runAfterDelay(55, () -> {
            assertRotation(h, tail, false);
            int remaining = access.engineFuel().remaining();
            h.runAfterDelay(10, () -> {
                h.assertTrue(remaining == access.engineFuel().remaining(), "Fuel burned during redstone pause");
                h.setBlock(new BlockPos(1,1,2), Blocks.AIR);
            });
        });
        h.runAfterDelay(90, () -> { assertRotation(h, tail, true); h.setBlock(shaft, Blocks.AIR); });
        h.runAfterDelay(120, () -> { assertRotation(h, tail, false); h.succeed(); });
    }

    private static void assertRotation(GameTestHelper h, BlockPos pos, boolean expected) {
        if (MechanicalPlatform.hasCreate()) {
            com.vvdd7232.elementalstaves.compat.create.CreateGameChecks.assertSpeed(h, pos, expected ? 40 : 0);
        } else {
            h.assertTrue(h.getBlockState(pos).getValue(DriveShaftBlock.ROTATING) == expected, "Incorrect standalone rotation");
        }
    }

    @GameTest(template = "empty", timeoutTicks = 220)
    public static void createNetworkAndStress(GameTestHelper h) {
        if (!MechanicalPlatform.hasCreate()) { h.succeed(); return; }
        com.vvdd7232.elementalstaves.compat.create.CreateGameChecks.networkAndStress(h);
    }
}
