package com.vvdd7232.elementalstaves;

import com.mojang.logging.LogUtils;
import com.vvdd7232.elementalstaves.item.MechanicalBlockItem;
import com.vvdd7232.elementalstaves.mechanical.CoalEngineBlock;
import com.vvdd7232.elementalstaves.mechanical.CoalEngineBlockEntity;
import com.vvdd7232.elementalstaves.mechanical.DriveShaftBlock;
import com.vvdd7232.elementalstaves.mechanical.DriveShaftBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.vvdd7232.elementalstaves.item.AirStaffItem;
import com.vvdd7232.elementalstaves.item.AquaStaffItem;
import com.vvdd7232.elementalstaves.item.EarthStaffItem;
import com.vvdd7232.elementalstaves.item.FireStaffItem;
import com.vvdd7232.elementalstaves.item.LifeStaffItem;
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
import net.minecraft.world.level.block.RotatedPillarBlock;
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

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredBlock<CoalEngineBlock> COAL_ENGINE = BLOCKS.register("coal_engine",
            () -> new CoalEngineBlock(copiedMineableProperties(Blocks.IRON_BLOCK)
                    .lightLevel(state -> state.getValue(CoalEngineBlock.LIT) ? 8 : 0)));
    public static final DeferredBlock<DriveShaftBlock> DRIVE_SHAFT = BLOCKS.register("drive_shaft",
            () -> new DriveShaftBlock(copiedMineableProperties(Blocks.IRON_BLOCK).noOcclusion()));
    public static final DeferredItem<BlockItem> COAL_ENGINE_ITEM = ITEMS.register("coal_engine",
            () -> new MechanicalBlockItem(COAL_ENGINE.get(), new Item.Properties(), "coal_engine"));
    public static final DeferredItem<BlockItem> DRIVE_SHAFT_ITEM = ITEMS.register("drive_shaft",
            () -> new MechanicalBlockItem(DRIVE_SHAFT.get(), new Item.Properties(), "drive_shaft"));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoalEngineBlockEntity>> COAL_ENGINE_ENTITY =
            BLOCK_ENTITIES.register("coal_engine", () -> BlockEntityType.Builder.of(CoalEngineBlockEntity::new, COAL_ENGINE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DriveShaftBlockEntity>> DRIVE_SHAFT_ENTITY =
            BLOCK_ENTITIES.register("drive_shaft", () -> BlockEntityType.Builder.of(DriveShaftBlockEntity::new, DRIVE_SHAFT.get()).build(null));

    // Natural and storage blocks. The ore requires at least an iron pickaxe to drop its resource.
    public static final DeferredBlock<Block> ELEMENTAL_ORE = BLOCKS.registerSimpleBlock(
            "elemental_ore",
            copiedMineableProperties(Blocks.DIAMOND_ORE)
    );
    public static final DeferredBlock<Block> DEEPSLATE_ELEMENTAL_ORE = BLOCKS.registerSimpleBlock(
            "deepslate_elemental_ore",
            copiedMineableProperties(Blocks.DEEPSLATE_DIAMOND_ORE)
    );
    public static final DeferredBlock<Block> ELEMENTAL_BLOCK = BLOCKS.registerSimpleBlock(
            "elemental_block",
            copiedMineableProperties(Blocks.IRON_BLOCK)
    );

    // v1.3 building and utility blocks: crystal storage, glowing lamp, rotatable pillar,
    // chiseled decoration and a charged block that can power a vanilla beacon.
    public static final DeferredBlock<Block> RAW_ELEMENTAL_BLOCK = BLOCKS.registerSimpleBlock(
            "raw_elemental_block",
            copiedMineableProperties(Blocks.RAW_IRON_BLOCK)
    );
    public static final DeferredBlock<Block> ELEMENTAL_LAMP = BLOCKS.registerSimpleBlock(
            "elemental_lamp",
            BlockBehaviour.Properties.ofFullCopy(Blocks.SEA_LANTERN).lightLevel(state -> 15)
    );
    public static final DeferredBlock<Block> ELEMENTAL_PILLAR = BLOCKS.register(
            "elemental_pillar",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.QUARTZ_PILLAR))
    );
    public static final DeferredBlock<Block> CHISELED_ELEMENTAL_BLOCK = BLOCKS.registerSimpleBlock(
            "chiseled_elemental_block",
            BlockBehaviour.Properties.ofFullCopy(Blocks.CHISELED_QUARTZ_BLOCK)
    );
    public static final DeferredBlock<Block> CHARGED_ELEMENTAL_BLOCK = BLOCKS.registerSimpleBlock(
            "charged_elemental_block",
            copiedMineableProperties(Blocks.IRON_BLOCK).lightLevel(state -> 7)
    );

    public static final DeferredItem<BlockItem> ELEMENTAL_ORE_ITEM = ITEMS.registerSimpleBlockItem(ELEMENTAL_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_ELEMENTAL_ORE_ITEM = ITEMS.registerSimpleBlockItem(DEEPSLATE_ELEMENTAL_ORE);
    public static final DeferredItem<BlockItem> ELEMENTAL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(ELEMENTAL_BLOCK);
    public static final DeferredItem<BlockItem> RAW_ELEMENTAL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(RAW_ELEMENTAL_BLOCK);
    public static final DeferredItem<BlockItem> ELEMENTAL_LAMP_ITEM = ITEMS.registerSimpleBlockItem(ELEMENTAL_LAMP);
    public static final DeferredItem<BlockItem> ELEMENTAL_PILLAR_ITEM = ITEMS.registerSimpleBlockItem(ELEMENTAL_PILLAR);
    public static final DeferredItem<BlockItem> CHISELED_ELEMENTAL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(CHISELED_ELEMENTAL_BLOCK);
    public static final DeferredItem<BlockItem> CHARGED_ELEMENTAL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(CHARGED_ELEMENTAL_BLOCK);
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
    public static final DeferredItem<AquaStaffItem> AQUA_STAFF = ITEMS.register("aqua_staff", AquaStaffItem::new);
    public static final DeferredItem<AirStaffItem> AIR_STAFF = ITEMS.register("air_staff", AirStaffItem::new);
    public static final DeferredItem<LifeStaffItem> LIFE_STAFF = ITEMS.register("life_staff", LifeStaffItem::new);

    public ElementalStaves(IEventBus modEventBus) {
        ARMOR_MATERIALS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        modEventBus.addListener(this::addCreativeItems);
        LOGGER.info("Elemental Staves loaded: elemental staves, Elemental Steel ore, tools, armor and coal engines and mechanical shafts.");
    }

    /**
     * Preserve vanilla physical settings while binding against the real 1.21.1
     * {@code ofFullCopy(BlockBehaviour)} descriptor, rather than a Block-specific overload.
     */
    private static BlockBehaviour.Properties copiedMineableProperties(BlockBehaviour source) {
        return BlockBehaviour.Properties.ofFullCopy(source).requiresCorrectToolForDrops();
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
            event.accept(RAW_ELEMENTAL_BLOCK_ITEM);
            event.accept(ELEMENTAL_BLOCK_ITEM);
            event.accept(CHISELED_ELEMENTAL_BLOCK_ITEM);
            event.accept(ELEMENTAL_PILLAR_ITEM);
            event.accept(CHARGED_ELEMENTAL_BLOCK_ITEM);
            event.accept(ELEMENTAL_LAMP_ITEM);
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(RAW_ELEMENTAL);
            event.accept(ELEMENTAL_INGOT);
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ELEMENTAL_PICKAXE);
            event.accept(ELEMENTAL_AXE);
            event.accept(ELEMENTAL_SHOVEL);
            event.accept(ELEMENTAL_HOE);
        } else if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(COAL_ENGINE_ITEM);
            event.accept(DRIVE_SHAFT_ITEM);
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ELEMENTAL_SWORD);
            event.accept(ELEMENTAL_HELMET);
            event.accept(ELEMENTAL_CHESTPLATE);
            event.accept(ELEMENTAL_LEGGINGS);
            event.accept(ELEMENTAL_BOOTS);
            event.accept(FIRE_STAFF);
            event.accept(LIGHTNING_STAFF);
            event.accept(EARTH_STAFF);
            event.accept(AQUA_STAFF);
            event.accept(AIR_STAFF);
            event.accept(LIFE_STAFF);
        }
    }
}
