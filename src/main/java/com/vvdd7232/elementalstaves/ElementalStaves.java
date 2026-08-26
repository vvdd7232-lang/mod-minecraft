package com.vvdd7232.elementalstaves;

import com.mojang.logging.LogUtils;
import com.vvdd7232.elementalstaves.item.EarthStaffItem;
import com.vvdd7232.elementalstaves.item.FireStaffItem;
import com.vvdd7232.elementalstaves.item.LightningStaffItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

/** NeoForge entry point and registries for Elemental Staves and Elemental Steel equipment. */
@Mod(ElementalStaves.MOD_ID)
public final class ElementalStaves {
    public static final String MOD_ID = "elementalstaves";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, MOD_ID);

    // Natural and storage blocks. The ore requires at least an iron pickaxe to drop its resource.
    public static final DeferredBlock<Block> ELEMENTAL_ORE = BLOCKS.registerSimpleBlock(
            "elemental_ore",
            BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE).requiresCorrectToolForDrops()
    );
    public static final DeferredBlock<Block> DEEPSLATE_ELEMENTAL_ORE = BLOCKS.registerSimpleBlock(
            "deepslate_elemental_ore",
            BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE).requiresCorrectToolForDrops()
    );
    public static final DeferredBlock<Block> ELEMENTAL_BLOCK = BLOCKS.registerSimpleBlock(
            "elemental_block",
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops()
    );

    public static final DeferredItem<BlockItem> ELEMENTAL_ORE_ITEM = ITEMS.registerSimpleBlockItem(ELEMENTAL_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_ELEMENTAL_ORE_ITEM = ITEMS.registerSimpleBlockItem(DEEPSLATE_ELEMENTAL_ORE);
    public static final DeferredItem<BlockItem> ELEMENTAL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(ELEMENTAL_BLOCK);
    public static final DeferredItem<Item> RAW_ELEMENTAL = ITEMS.registerSimpleItem("raw_elemental");
    public static final DeferredItem<Item> ELEMENTAL_INGOT = ITEMS.registerSimpleItem("elemental_ingot");

    // The armor material is a registered data-driven registry entry; every armor item holds this entry directly.
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ELEMENTAL_ARMOR_MATERIAL = ARMOR_MATERIALS.register(
            "elemental",
            () -> new ArmorMaterial(
                    Map.of(
                            ArmorItem.Type.BOOTS, 2,
                            ArmorItem.Type.LEGGINGS, 5,
                            ArmorItem.Type.CHESTPLATE, 7,
                            ArmorItem.Type.HELMET, 3,
                            ArmorItem.Type.BODY, 6
                    ),
                    18,
                    SoundEvents.ARMOR_EQUIP_DIAMOND,
                    () -> Ingredient.of(ELEMENTAL_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(MOD_ID, "elemental"))),
                    1.0F,
                    0.0F
            )
    );

    // Elemental Steel equipment is intentionally between iron and diamond in raw combat and mining power.
    public static final DeferredItem<SwordItem> ELEMENTAL_SWORD = ITEMS.register("elemental_sword", () -> new SwordItem(
            ElementalMaterials.ELEMENTAL_TIER,
            new Item.Properties().attributes(SwordItem.createAttributes(ElementalMaterials.ELEMENTAL_TIER, 3, -2.4F))
    ));
    public static final DeferredItem<PickaxeItem> ELEMENTAL_PICKAXE = ITEMS.register("elemental_pickaxe", () -> new PickaxeItem(
            ElementalMaterials.ELEMENTAL_TIER,
            new Item.Properties().attributes(DiggerItem.createAttributes(ElementalMaterials.ELEMENTAL_TIER, 1.0F, -2.8F))
    ));
    public static final DeferredItem<AxeItem> ELEMENTAL_AXE = ITEMS.register("elemental_axe", () -> new AxeItem(
            ElementalMaterials.ELEMENTAL_TIER,
            new Item.Properties().attributes(DiggerItem.createAttributes(ElementalMaterials.ELEMENTAL_TIER, 5.0F, -3.0F))
    ));
    public static final DeferredItem<ShovelItem> ELEMENTAL_SHOVEL = ITEMS.register("elemental_shovel", () -> new ShovelItem(
            ElementalMaterials.ELEMENTAL_TIER,
            new Item.Properties().attributes(DiggerItem.createAttributes(ElementalMaterials.ELEMENTAL_TIER, 1.5F, -3.0F))
    ));
    public static final DeferredItem<HoeItem> ELEMENTAL_HOE = ITEMS.register("elemental_hoe", () -> new HoeItem(
            ElementalMaterials.ELEMENTAL_TIER,
            new Item.Properties().attributes(DiggerItem.createAttributes(ElementalMaterials.ELEMENTAL_TIER, -2.0F, -1.0F))
    ));

    public static final DeferredItem<ArmorItem> ELEMENTAL_HELMET = ITEMS.register("elemental_helmet", () -> new ArmorItem(
            ELEMENTAL_ARMOR_MATERIAL, ArmorItem.Type.HELMET, armorProperties(ArmorItem.Type.HELMET)
    ));
    public static final DeferredItem<ArmorItem> ELEMENTAL_CHESTPLATE = ITEMS.register("elemental_chestplate", () -> new ArmorItem(
            ELEMENTAL_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, armorProperties(ArmorItem.Type.CHESTPLATE)
    ));
    public static final DeferredItem<ArmorItem> ELEMENTAL_LEGGINGS = ITEMS.register("elemental_leggings", () -> new ArmorItem(
            ELEMENTAL_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, armorProperties(ArmorItem.Type.LEGGINGS)
    ));
    public static final DeferredItem<ArmorItem> ELEMENTAL_BOOTS = ITEMS.register("elemental_boots", () -> new ArmorItem(
            ELEMENTAL_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, armorProperties(ArmorItem.Type.BOOTS)
    ));

    public static final DeferredItem<FireStaffItem> FIRE_STAFF = ITEMS.register("fire_staff", FireStaffItem::new);
    public static final DeferredItem<LightningStaffItem> LIGHTNING_STAFF = ITEMS.register("lightning_staff", LightningStaffItem::new);
    public static final DeferredItem<EarthStaffItem> EARTH_STAFF = ITEMS.register("earth_staff", EarthStaffItem::new);

    public ElementalStaves(IEventBus modEventBus) {
        ARMOR_MATERIALS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(this::addCreativeItems);
        LOGGER.info("Elemental Staves loaded: staves, Elemental Steel ore, tools and armor are ready.");
    }

    private static Item.Properties armorProperties(ArmorItem.Type type) {
        // Base 25 gives a full set 275 / 400 / 375 / 325 durability (helmet / chestplate / leggings / boots).
        return new Item.Properties().durability(type.getDurability(25));
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ELEMENTAL_ORE_ITEM);
            event.accept(DEEPSLATE_ELEMENTAL_ORE_ITEM);
        } else if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ELEMENTAL_BLOCK_ITEM);
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(RAW_ELEMENTAL);
            event.accept(ELEMENTAL_INGOT);
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ELEMENTAL_PICKAXE);
            event.accept(ELEMENTAL_AXE);
            event.accept(ELEMENTAL_SHOVEL);
            event.accept(ELEMENTAL_HOE);
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ELEMENTAL_SWORD);
            event.accept(ELEMENTAL_HELMET);
            event.accept(ELEMENTAL_CHESTPLATE);
            event.accept(ELEMENTAL_LEGGINGS);
            event.accept(ELEMENTAL_BOOTS);
            event.accept(FIRE_STAFF);
            event.accept(LIGHTNING_STAFF);
            event.accept(EARTH_STAFF);
        }
    }
}
