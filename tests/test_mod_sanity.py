#!/usr/bin/env python3
"""Regression checks for the 1.21.1 datapack and Java API wiring."""

from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "src/main/resources"
JAVA = ROOT / "src/main/java/com/vvdd7232/elementalstaves"


class StaffMathTests(unittest.TestCase):
    def test_squared_block_distance_matches_java_helper(self) -> None:
        def squared_block_distance(x1: int, y1: int, z1: int, x2: int, y2: int, z2: int) -> float:
            dx = float(x1) - x2
            dy = float(y1) - y2
            dz = float(z1) - z2
            return dx * dx + dy * dy + dz * dz

        self.assertEqual(0.0, squared_block_distance(0, 0, 0, 0, 0, 0))
        self.assertEqual(16.0, squared_block_distance(0, 0, 0, 4, 0, 0))
        self.assertEqual(3.0, squared_block_distance(1, 1, 1, 2, 2, 2))
        self.assertTrue(squared_block_distance(0, 0, 0, 16, 0, 0) <= 16 * 16)
        self.assertFalse(squared_block_distance(0, 0, 0, 17, 0, 0) <= 16 * 16)


class DatapackTests(unittest.TestCase):
    def test_every_json_file_parses(self) -> None:
        files = list(RESOURCES.rglob("*.json"))
        self.assertGreater(len(files), 40)
        for path in files:
            with self.subTest(path=str(path.relative_to(ROOT))):
                json.loads(path.read_text(encoding="utf-8"))

    def test_recipe_unlocks_use_1_21_advancement_folder(self) -> None:
        self.assertFalse((RESOURCES / "data/elementalstaves/advancements").exists())
        self.assertTrue((RESOURCES / "data/elementalstaves/advancement/recipes/combat/fire_staff.json").is_file())

    def test_ore_loot_supports_silk_touch_and_fortune(self) -> None:
        for name in ("elemental_ore.json", "deepslate_elemental_ore.json"):
            data = json.loads((RESOURCES / "data/elementalstaves/loot_table/blocks" / name).read_text(encoding="utf-8"))
            blob = json.dumps(data)
            self.assertIn("minecraft:silk_touch", blob)
            self.assertIn("minecraft:fortune", blob)
            self.assertIn("minecraft:ore_drops", blob)
            self.assertIn("minecraft:alternatives", blob)

    def test_storage_block_loot_survives_explosions(self) -> None:
        data = json.loads(
            (RESOURCES / "data/elementalstaves/loot_table/blocks/elemental_block.json").read_text(encoding="utf-8")
        )
        self.assertIn("minecraft:survives_explosion", json.dumps(data))

    def test_tools_are_in_vanilla_tool_tags(self) -> None:
        mapping = {
            "pickaxes": "elementalstaves:elemental_pickaxe",
            "swords": "elementalstaves:elemental_sword",
            "axes": "elementalstaves:elemental_axe",
            "shovels": "elementalstaves:elemental_shovel",
            "hoes": "elementalstaves:elemental_hoe",
        }
        for tag_name, item_id in mapping.items():
            data = json.loads((RESOURCES / f"data/minecraft/tags/item/{tag_name}.json").read_text(encoding="utf-8"))
            self.assertIn(item_id, data["values"])

    def test_staves_are_unbreaking_enchantable(self) -> None:
        data = json.loads(
            (RESOURCES / "data/minecraft/tags/item/enchantable/durability.json").read_text(encoding="utf-8")
        )
        for staff in (
            "elementalstaves:fire_staff",
            "elementalstaves:lightning_staff",
            "elementalstaves:earth_staff",
        ):
            self.assertIn(staff, data["values"])

    def test_incorrect_for_elemental_tool_matches_diamond_harvest(self) -> None:
        data = json.loads(
            (RESOURCES / "data/elementalstaves/tags/block/incorrect_for_elemental_tool.json").read_text(encoding="utf-8")
        )
        self.assertIn("#minecraft:incorrect_for_diamond_tool", data["values"])
        self.assertNotIn("remove", data)

    def test_ore_blocks_can_be_smelted(self) -> None:
        for recipe in (
            "elemental_ingot_from_smelting_ore.json",
            "elemental_ingot_from_smelting_deepslate_ore.json",
            "elemental_ingot_from_blasting_ore.json",
            "elemental_ingot_from_blasting_deepslate_ore.json",
        ):
            path = RESOURCES / "data/elementalstaves/recipe" / recipe
            self.assertTrue(path.is_file(), recipe)
            data = json.loads(path.read_text(encoding="utf-8"))
            self.assertEqual("elementalstaves:elemental_ingot", data["result"]["id"])


class JavaApiTests(unittest.TestCase):
    def _read(self, relative: str) -> str:
        return (JAVA / relative).read_text(encoding="utf-8")

    def test_armor_binds_attribute_modifier_component(self) -> None:
        source = self._read("ElementalStaves.java")
        self.assertIn("DataComponents.ATTRIBUTE_MODIFIERS", source)
        self.assertIn("Attributes.ARMOR", source)
        self.assertIn("Attributes.ARMOR_TOUGHNESS", source)
        self.assertIn("ItemAttributeModifiers", source)

    def test_tools_bind_attribute_modifier_component(self) -> None:
        source = self._read("ElementalStaves.java")
        self.assertIn(".component(DataComponents.ATTRIBUTE_MODIFIERS, modifiers)", source)
        self.assertIn("SwordItem.createAttributes", source)
        self.assertIn("DiggerItem.createAttributes", source)

    def test_block_copy_uses_block_behaviour_descriptor(self) -> None:
        source = self._read("ElementalStaves.java")
        self.assertIn("BlockBehaviour.Properties.ofFullCopy(source)", source)

    def test_staffs_use_item_cooldowns_and_tooltip_component(self) -> None:
        for relative in (
            "item/BaseStaffItem.java",
            "item/FireStaffItem.java",
            "item/LightningStaffItem.java",
            "item/EarthStaffItem.java",
        ):
            source = self._read(relative)
            self.assertIn("import net.minecraft.world.item.ItemCooldowns;", source)
            if relative != "item/BaseStaffItem.java":
                self.assertIn("import net.minecraft.network.chat.Component;", source)
                self.assertIn("TooltipFlag", source)

    def test_earth_staff_preserves_custom_data_set_return_value(self) -> None:
        source = self._read("item/EarthStaffItem.java")
        self.assertIn("stack.set(DataComponents.CUSTOM_DATA, CustomData.of(selectionData))", source)
        self.assertIn("StaffMath.squaredBlockDistance", source)
        self.assertIn("isMultiBlock", source)
        self.assertIn("level.setBlock(destination, Blocks.AIR.defaultBlockState()", source)

    def test_lightning_uses_move_to_and_entity_type_create(self) -> None:
        source = self._read("item/LightningStaffItem.java")
        self.assertIn("EntityType.LIGHTNING_BOLT.create(serverLevel)", source)
        self.assertIn("lightning.moveTo(Vec3.atBottomCenterOf(target))", source)

    def test_client_side_uses_field_not_method(self) -> None:
        for relative in (
            "item/FireStaffItem.java",
            "item/LightningStaffItem.java",
            "item/EarthStaffItem.java",
        ):
            source = self._read(relative)
            self.assertIn("level.isClientSide", source)
            self.assertNotIn("level.isClientSide()", source)


if __name__ == "__main__":
    suite = unittest.defaultTestLoader.loadTestsFromModule(sys.modules[__name__])
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    sys.exit(0 if result.wasSuccessful() else 1)
