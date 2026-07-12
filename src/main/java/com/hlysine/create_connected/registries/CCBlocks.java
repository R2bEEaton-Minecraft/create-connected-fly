package com.hlysine.create_connected.registries;

import com.hlysine.create_connected.compat.DyeDepotCompat;
import com.hlysine.create_connected.compat.Mods;
import com.hlysine.create_connected.compat.SimCompatRegistry;
import com.hlysine.create_connected.config.CStress;
import com.hlysine.create_connected.config.FeatureCategory;
import com.hlysine.create_connected.config.FeatureToggle;
import com.hlysine.create_connected.content.WrenchableBlock;
import com.hlysine.create_connected.content.brake.BrakeBlock;
import com.hlysine.create_connected.content.brasschute.BrassChuteBlock;
import com.hlysine.create_connected.content.brassgearbox.BrassGearboxBlock;
import com.hlysine.create_connected.content.centrifugalclutch.CentrifugalClutchBlock;
import com.hlysine.create_connected.content.chaincogwheel.ChainCogwheelBlock;
import com.hlysine.create_connected.content.copycat.beam.CopycatBeamBlock;
import com.hlysine.create_connected.content.copycat.block.CopycatBlockBlock;
import com.hlysine.create_connected.content.copycat.board.CopycatBoardBlock;
import com.hlysine.create_connected.content.copycat.fence.CopycatFenceBlock;
import com.hlysine.create_connected.content.copycat.fence.WrappedFenceBlock;
import com.hlysine.create_connected.content.copycat.fencegate.CopycatFenceGateBlock;
import com.hlysine.create_connected.content.copycat.fencegate.WrappedFenceGateBlock;
import com.hlysine.create_connected.content.copycat.slab.CopycatSlabBlock;
import com.hlysine.create_connected.content.copycat.stairs.CopycatStairsBlock;
import com.hlysine.create_connected.content.copycat.stairs.WrappedStairsBlock;
import com.hlysine.create_connected.content.copycat.verticalstep.CopycatVerticalStepBlock;
import com.hlysine.create_connected.content.copycat.wall.CopycatWallBlock;
import com.hlysine.create_connected.content.copycat.wall.WrappedWallBlock;
import com.hlysine.create_connected.content.crankwheel.CrankWheelBlock;
import com.hlysine.create_connected.content.crankwheel.CrankWheelItem;
import com.hlysine.create_connected.content.crossconnector.CrossConnectorBlock;
import com.hlysine.create_connected.content.crossconnector.EncasedCrossConnectorBlock;
import com.hlysine.create_connected.content.dashboard.DashboardBlock;
import com.hlysine.create_connected.content.fancatalyst.FanCatalystRotatingHeadBlock;
import com.hlysine.create_connected.content.fluidvessel.FluidVesselBlock;
import com.hlysine.create_connected.content.fluidvessel.FluidVesselItem;
import com.hlysine.create_connected.content.freewheelclutch.FreewheelClutchBlock;
import com.hlysine.create_connected.content.inventoryaccessport.InventoryAccessPortBlock;
import com.hlysine.create_connected.content.inventorybridge.InventoryBridgeBlock;
import com.hlysine.create_connected.content.invertedclutch.InvertedClutchBlock;
import com.hlysine.create_connected.content.invertedgearshift.InvertedGearshiftBlock;
import com.hlysine.create_connected.content.itemsilo.ItemSiloBlock;
import com.hlysine.create_connected.content.itemsilo.ItemSiloItem;
import com.hlysine.create_connected.content.kineticbattery.KineticBatteryBlock;
import com.hlysine.create_connected.content.kineticbattery.KineticBatteryBlockItem;
import com.hlysine.create_connected.content.kineticbridge.KineticBridgeBlock;
import com.hlysine.create_connected.content.kineticbridge.KineticBridgeBlockItem;
import com.hlysine.create_connected.content.kineticbridge.KineticBridgeDestinationBlock;
import com.hlysine.create_connected.content.linkedtransmitter.LinkedAnalogLeverBlock;
import com.hlysine.create_connected.content.linkedtransmitter.LinkedButtonBlock;
import com.hlysine.create_connected.content.linkedtransmitter.LinkedLeverBlock;
import com.hlysine.create_connected.content.linkedtransmitter.LinkedTransmitterItem;
import com.hlysine.create_connected.content.overstressclutch.OverstressClutchBlock;
import com.hlysine.create_connected.content.parallelgearbox.ParallelGearboxBlock;
import com.hlysine.create_connected.content.sequencedpulsegenerator.SequencedPulseGeneratorBlock;
import com.hlysine.create_connected.content.shearpin.ShearPinBlock;
import com.hlysine.create_connected.content.sixwaygearbox.SixWayGearboxBlock;
import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.api.connectivity.ConnectivityHandler;
import com.zurrtum.create.api.contraption.BlockMovementChecks;
import com.zurrtum.create.api.contraption.storage.fluid.MountedFluidStorageType;
import com.zurrtum.create.api.contraption.storage.item.MountedItemStorageType;
import com.zurrtum.create.api.behaviour.display.DisplaySource;
import com.zurrtum.create.api.stress.BlockStressValues;
import com.zurrtum.create.content.decoration.encasing.EncasingRegistry;
import com.zurrtum.create.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

// Original was a Registrate builder chain (REGISTRATE.block(...).blockstate(...).item()...register()).
// Registrate does not exist in Create Fly - see PORTING_NOTES.md "CORRECTIONS" section. Converted to
// direct registration via CCRegistrate (mirrors CreateModAddon's ModBlocks.java pattern). All
// .blockstate()/.model()/.lang()/.loot()/.tag() calls are dropped: they were pure datagen (asset/tag
// JSON generation), and this mod's assets+tags are already committed as static JSON under
// src/generated/resources and src/main/resources. Real runtime behavior (stress values, feature
// toggles, connectivity, movement checks, mounted storage, display sources) is preserved as explicit
// statements after each block's registration.
public class CCBlocks {
    public static final ChainCogwheelBlock ENCASED_CHAIN_COGWHEEL =
            CCRegistrate.block("encased_chain_cogwheel", ChainCogwheelBlock::new,
                    CCSharedProperties.stone().noOcclusion().mapColor(MapColor.PODZOL));
    static {
        CStress.setNoImpact(ENCASED_CHAIN_COGWHEEL);
        FeatureToggle.register(id("encased_chain_cogwheel"), FeatureCategory.KINETIC);
        CCRegistrate.blockItem(ENCASED_CHAIN_COGWHEEL, "encased_chain_cogwheel");
    }

    public static final CrankWheelBlock.Small CRANK_WHEEL =
            CCRegistrate.block("crank_wheel", CrankWheelBlock.Small::new, CCSharedProperties.wooden().mapColor(MapColor.PODZOL));
    static {
        CStress.setCapacity(CRANK_WHEEL, 8.0);
        BlockStressValues.setGeneratorSpeed(CRANK_WHEEL, 32);
        FeatureToggle.register(id("crank_wheel"), FeatureCategory.KINETIC);
        CCRegistrate.blockItem(CRANK_WHEEL, "crank_wheel", (b, p) -> new CrankWheelItem((CrankWheelBlock) b, p));
    }

    public static final CrankWheelBlock.Large LARGE_CRANK_WHEEL =
            CCRegistrate.block("large_crank_wheel", CrankWheelBlock.Large::new, CCSharedProperties.wooden().mapColor(MapColor.PODZOL));
    static {
        CStress.setCapacity(LARGE_CRANK_WHEEL, 8.0);
        BlockStressValues.setGeneratorSpeed(LARGE_CRANK_WHEEL, 32);
        FeatureToggle.register(id("large_crank_wheel"), FeatureCategory.KINETIC);
        CCRegistrate.blockItem(LARGE_CRANK_WHEEL, "large_crank_wheel", (b, p) -> new CrankWheelItem((CrankWheelBlock) b, p));
    }

    public static final ParallelGearboxBlock PARALLEL_GEARBOX =
            CCRegistrate.block("parallel_gearbox", ParallelGearboxBlock::new, CCSharedProperties.stone().noOcclusion().mapColor(MapColor.PODZOL));
    static {
        CStress.setNoImpact(PARALLEL_GEARBOX);
        FeatureToggle.register(id("parallel_gearbox"), FeatureCategory.KINETIC);
        // CT behaviour (CreateRegistrate.connectedTextures/casingConnectivity) is client-only
        // rendering in Create Fly (com.zurrtum.create.client.content.decoration.encasing) - still
        // needs to be wired from a client-sourceset class; see PORTING_NOTES.md follow-ups.
        CCRegistrate.blockItem(PARALLEL_GEARBOX, "parallel_gearbox");
    }

    public static final SixWayGearboxBlock SIX_WAY_GEARBOX =
            CCRegistrate.block("six_way_gearbox", SixWayGearboxBlock::new, CCSharedProperties.stone().noOcclusion().mapColor(MapColor.PODZOL));
    static {
        CStress.setNoImpact(SIX_WAY_GEARBOX);
        FeatureToggle.register(id("six_way_gearbox"), FeatureCategory.KINETIC);
        CCRegistrate.blockItem(SIX_WAY_GEARBOX, "six_way_gearbox");
    }

    public static final CrossConnectorBlock CROSS_CONNECTOR =
            CCRegistrate.block("cross_connector", CrossConnectorBlock::new, CCSharedProperties.stone().noOcclusion().mapColor(MapColor.PODZOL));
    static {
        FeatureToggle.register(id("cross_connector"), FeatureCategory.KINETIC);
        CCRegistrate.blockItem(CROSS_CONNECTOR, "cross_connector");
    }

    public static final EncasedCrossConnectorBlock ANDESITE_ENCASED_CROSS_CONNECTOR =
            CCRegistrate.block("andesite_encased_cross_connector", p -> new EncasedCrossConnectorBlock(p, () -> AllBlocks.ANDESITE_CASING),
                    CCSharedProperties.stone().mapColor(MapColor.PODZOL));
    static {
        EncasingRegistry.addVariant(CROSS_CONNECTOR, ANDESITE_ENCASED_CROSS_CONNECTOR);
        FeatureToggle.registerDependent(id("andesite_encased_cross_connector"), id("cross_connector"), FeatureCategory.KINETIC);
        CCRegistrate.blockItem(ANDESITE_ENCASED_CROSS_CONNECTOR, "andesite_encased_cross_connector");
    }

    public static final EncasedCrossConnectorBlock BRASS_ENCASED_CROSS_CONNECTOR =
            CCRegistrate.block("brass_encased_cross_connector", p -> new EncasedCrossConnectorBlock(p, () -> AllBlocks.BRASS_CASING),
                    CCSharedProperties.stone().mapColor(MapColor.TERRACOTTA_BROWN));
    static {
        EncasingRegistry.addVariant(CROSS_CONNECTOR, BRASS_ENCASED_CROSS_CONNECTOR);
        FeatureToggle.registerDependent(id("brass_encased_cross_connector"), id("cross_connector"), FeatureCategory.KINETIC);
        CCRegistrate.blockItem(BRASS_ENCASED_CROSS_CONNECTOR, "brass_encased_cross_connector");
    }

    public static final OverstressClutchBlock OVERSTRESS_CLUTCH =
            CCRegistrate.block("overstress_clutch", OverstressClutchBlock::new, CCSharedProperties.stone().noOcclusion().mapColor(MapColor.PODZOL));
    static {
        CStress.setNoImpact(OVERSTRESS_CLUTCH);
        FeatureToggle.register(id("overstress_clutch"), FeatureCategory.KINETIC);
        CCRegistrate.blockItem(OVERSTRESS_CLUTCH, "overstress_clutch");
    }

    public static final ShearPinBlock SHEAR_PIN =
            CCRegistrate.block("shear_pin", ShearPinBlock::new, CCSharedProperties.stone().mapColor(MapColor.METAL).forceSolidOn());
    static {
        CStress.setNoImpact(SHEAR_PIN);
        FeatureToggle.register(id("shear_pin"), FeatureCategory.KINETIC);
        CCRegistrate.blockItem(SHEAR_PIN, "shear_pin");
    }

    public static final InvertedClutchBlock INVERTED_CLUTCH =
            CCRegistrate.block("inverted_clutch", InvertedClutchBlock::new, CCSharedProperties.stone().noOcclusion().mapColor(MapColor.PODZOL));
    static {
        CStress.setNoImpact(INVERTED_CLUTCH);
        FeatureToggle.register(id("inverted_clutch"), FeatureCategory.KINETIC);
        CCRegistrate.blockItem(INVERTED_CLUTCH, "inverted_clutch");
    }

    public static final InvertedGearshiftBlock INVERTED_GEARSHIFT =
            CCRegistrate.block("inverted_gearshift", InvertedGearshiftBlock::new, CCSharedProperties.stone().noOcclusion().mapColor(MapColor.PODZOL));
    static {
        CStress.setNoImpact(INVERTED_GEARSHIFT);
        FeatureToggle.register(id("inverted_gearshift"), FeatureCategory.KINETIC);
        CCRegistrate.blockItem(INVERTED_GEARSHIFT, "inverted_gearshift");
    }

    public static final CentrifugalClutchBlock CENTRIFUGAL_CLUTCH =
            CCRegistrate.block("centrifugal_clutch", CentrifugalClutchBlock::new, CCSharedProperties.stone().noOcclusion().mapColor(MapColor.PODZOL));
    static {
        CStress.setNoImpact(CENTRIFUGAL_CLUTCH);
        FeatureToggle.register(id("centrifugal_clutch"), FeatureCategory.KINETIC);
        CCRegistrate.blockItem(CENTRIFUGAL_CLUTCH, "centrifugal_clutch");
    }

    public static final FreewheelClutchBlock FREEWHEEL_CLUTCH =
            CCRegistrate.block("freewheel_clutch", FreewheelClutchBlock::new, CCSharedProperties.stone().noOcclusion().mapColor(MapColor.PODZOL));
    static {
        CStress.setNoImpact(FREEWHEEL_CLUTCH);
        FeatureToggle.register(id("freewheel_clutch"), FeatureCategory.KINETIC);
        CCRegistrate.blockItem(FREEWHEEL_CLUTCH, "freewheel_clutch");
    }

    public static final KineticBridgeBlock KINETIC_BRIDGE =
            CCRegistrate.block("kinetic_bridge", KineticBridgeBlock::new, CCSharedProperties.stone().noOcclusion().mapColor(MapColor.TERRACOTTA_BROWN));
    static {
        FeatureToggle.register(id("kinetic_bridge"), FeatureCategory.KINETIC);
        BlockMovementChecks.registerAttachedCheck((state, world, pos, direction) -> {
            if (!(state.getBlock() instanceof KineticBridgeBlock))
                return BlockMovementChecks.CheckResult.PASS;
            if (state.getValue(KineticBridgeBlock.FACING) != direction)
                return BlockMovementChecks.CheckResult.PASS;
            return BlockMovementChecks.CheckResult.SUCCESS;
        });
        BlockMovementChecks.registerBrittleCheck(state -> {
            if (!(state.getBlock() instanceof KineticBridgeBlock))
                return BlockMovementChecks.CheckResult.PASS;
            return BlockMovementChecks.CheckResult.SUCCESS;
        });
        CCRegistrate.blockItem(KINETIC_BRIDGE, "kinetic_bridge", (b, p) -> new KineticBridgeBlockItem(b, p));
    }

    public static final KineticBridgeDestinationBlock KINETIC_BRIDGE_DESTINATION =
            CCRegistrate.block("kinetic_bridge_destination", KineticBridgeDestinationBlock::new,
                    CCSharedProperties.stone().noOcclusion().mapColor(MapColor.TERRACOTTA_BROWN));
    static {
        FeatureToggle.registerDependent(id("kinetic_bridge_destination"), id("kinetic_bridge"), FeatureCategory.KINETIC);
        BlockMovementChecks.registerAttachedCheck((state, world, pos, direction) -> {
            if (!(state.getBlock() instanceof KineticBridgeDestinationBlock))
                return BlockMovementChecks.CheckResult.PASS;
            if (state.getValue(KineticBridgeDestinationBlock.FACING).getOpposite() != direction)
                return BlockMovementChecks.CheckResult.PASS;
            return BlockMovementChecks.CheckResult.SUCCESS;
        });
        // no item registered in the original (block-only)
    }

    public static final BrassGearboxBlock BRASS_GEARBOX =
            CCRegistrate.block("brass_gearbox", BrassGearboxBlock::new, CCSharedProperties.stone().noOcclusion().mapColor(MapColor.TERRACOTTA_BROWN));
    static {
        CStress.setNoImpact(BRASS_GEARBOX);
        FeatureToggle.register(id("brass_gearbox"), FeatureCategory.KINETIC);
        // CT behaviour: deferred to client conversion, see PORTING_NOTES.md
        CCRegistrate.blockItem(BRASS_GEARBOX, "brass_gearbox");
    }

    public static final BrakeBlock BRAKE =
            CCRegistrate.block("brake", BrakeBlock::new, CCSharedProperties.stone().noOcclusion().mapColor(MapColor.PODZOL));
    static {
        CStress.setNoImpact(BRAKE); // active stress is a separate config
        FeatureToggle.register(id("brake"), FeatureCategory.KINETIC);
        CCRegistrate.blockItem(BRAKE, "brake");
    }

    // BlockBehaviour.Properties has no .component(...) method at all (confirmed via javap - only
    // Item.Properties does; data components live on ItemStacks, not on Blocks/BlockStates) - the
    // matching call on the block-item's own Properties below (which does exist and compiles) already
    // covers the real default-value use case, so this one is dropped rather than replaced.
    public static final KineticBatteryBlock KINETIC_BATTERY =
            CCRegistrate.block("kinetic_battery", KineticBatteryBlock::new, CCSharedProperties.stone().noOcclusion().mapColor(MapColor.TERRACOTTA_BROWN));
    public static final KineticBatteryBlockItem KINETIC_BATTERY_ITEM =
            CCRegistrate.blockItem(KINETIC_BATTERY, "kinetic_battery", (b, p) -> new KineticBatteryBlockItem(b, p),
                    new net.minecraft.world.item.Item.Properties().component(CCDataComponents.KINETIC_BATTERY_CHARGE, 0.0));
    static {
        CStress.setCapacity(KINETIC_BATTERY, 32.0);
        CStress.setImpact(KINETIC_BATTERY, 64.0);
        FeatureToggle.register(id("kinetic_battery"), FeatureCategory.KINETIC);
        DisplaySource.BY_BLOCK.add(KINETIC_BATTERY, CCDisplaySources.KINETIC_BATTERY);
        // KineticBatteryOverrides.registerModelOverridesClient(KINETIC_BATTERY_ITEM) is called from
        // CreateConnectedClient.onInitializeClient() instead of here - it's client-only rendering
        // (ItemProperties.register), can't be reached from this main-sourceset class per Loom's
        // split source sets (see PORTING_NOTES.md).
    }

    public static final SequencedPulseGeneratorBlock SEQUENCED_PULSE_GENERATOR =
            CCRegistrate.block("sequenced_pulse_generator", SequencedPulseGeneratorBlock::new, CCSharedProperties.stone());
    static {
        FeatureToggle.register(id("sequenced_pulse_generator"), FeatureCategory.REDSTONE);
        CCRegistrate.blockItem(SEQUENCED_PULSE_GENERATOR, "sequenced_pulse_generator");
    }

    public static final Map<BlockSetType, LinkedButtonBlock> LINKED_BUTTONS = new HashMap<>();

    static {
        BlockSetType.values().forEach(type -> {
            Block button = RegisteredObjectsHelper.getBlock(Identifier.parse(type.name() + "_button"));
            if (button == null) return;
            if (!(button instanceof ButtonBlock buttonBlock))
                return;
            String namePath = type.name().contains(":") ? type.name().replace(':', '_') : type.name();
            LinkedButtonBlock linkedButton = CCRegistrate.block("linked_" + namePath + "_button",
                    properties -> new LinkedButtonBlock(properties, buttonBlock), net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(buttonBlock));
            LinkedTransmitterItem.MODULE_BLOCKS.add(linkedButton);
            PreciseItemUseOverrides.addBlock(linkedButton);
            LINKED_BUTTONS.put(type, linkedButton);
        });
    }

    public static final LinkedLeverBlock LINKED_LEVER =
            CCRegistrate.block("linked_lever", properties -> new LinkedLeverBlock(properties, (LeverBlock) Blocks.LEVER), net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(Blocks.LEVER));
    static {
        LinkedTransmitterItem.MODULE_BLOCKS.add(LINKED_LEVER);
        PreciseItemUseOverrides.addBlock(LINKED_LEVER);
    }

    public static final LinkedAnalogLeverBlock LINKED_ANALOG_LEVER =
            CCRegistrate.block("linked_analog_lever", properties -> new LinkedAnalogLeverBlock(properties, () -> AllBlocks.ANALOG_LEVER), net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(Blocks.LEVER));
    static {
        LinkedTransmitterItem.MODULE_BLOCKS.add(LINKED_ANALOG_LEVER);
        PreciseItemUseOverrides.addBlock(LINKED_ANALOG_LEVER);
    }

    public static final WrenchableBlock EMPTY_FAN_CATALYST = CCRegistrate.block("empty_fan_catalyst", WrenchableBlock::new,
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.TERRACOTTA_YELLOW).requiresCorrectToolForDrops().noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false));
    static {
        FeatureToggle.register(id("empty_fan_catalyst"), FeatureCategory.LOGISTICS);
        CCRegistrate.blockItem(EMPTY_FAN_CATALYST, "empty_fan_catalyst");
    }

    private static WrenchableBlock fanCatalyst(String path, int lightLevel) {
        WrenchableBlock block = CCRegistrate.block(path, WrenchableBlock::new,
                net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.TERRACOTTA_YELLOW).requiresCorrectToolForDrops().noOcclusion()
                        .lightLevel(s -> lightLevel).isRedstoneConductor((state, level, pos) -> false));
        FeatureToggle.registerDependent(id(path), id("empty_fan_catalyst"));
        CCRegistrate.blockItem(block, path);
        return block;
    }

    public static final WrenchableBlock FAN_BLASTING_CATALYST = fanCatalyst("fan_blasting_catalyst", 0);
    public static final WrenchableBlock FAN_SMOKING_CATALYST = fanCatalyst("fan_smoking_catalyst", 10);
    public static final WrenchableBlock FAN_SPLASHING_CATALYST = fanCatalyst("fan_splashing_catalyst", 0);
    public static final WrenchableBlock FAN_HAUNTING_CATALYST = fanCatalyst("fan_haunting_catalyst", 5);
    public static final WrenchableBlock FAN_FREEZING_CATALYST = fanCatalyst("fan_freezing_catalyst", 0);
    static {
        FeatureToggle.addCondition(id("fan_freezing_catalyst"), () -> Mods.GARNISHED.isLoaded() || Mods.DREAMS_DESIRES.isLoaded() || Mods.DRAGONS_PLUS.isLoaded());
    }
    public static final WrenchableBlock FAN_SEETHING_CATALYST = fanCatalyst("fan_seething_catalyst", 12);
    static {
        FeatureToggle.addCondition(id("fan_seething_catalyst"), Mods.DREAMS_DESIRES::isLoaded);
    }
    public static final WrenchableBlock FAN_SANDING_CATALYST = fanCatalyst("fan_sanding_catalyst", 0);
    static {
        FeatureToggle.addCondition(id("fan_sanding_catalyst"), () -> Mods.DREAMS_DESIRES.isLoaded() || Mods.DRAGONS_PLUS.isLoaded());
    }
    public static final WrenchableBlock FAN_ENRICHED_CATALYST = fanCatalyst("fan_enriched_catalyst", 13);
    static {
        FeatureToggle.addCondition(id("fan_enriched_catalyst"), Mods.NUCLEAR::isLoaded);
    }
    public static final WrenchableBlock FAN_ENDING_CATALYST_DRAGONS_BREATH = fanCatalyst("fan_ending_catalyst_dragons_breath", 15);
    static {
        FeatureToggle.addCondition(id("fan_ending_catalyst_dragons_breath"), Mods.DRAGONS_PLUS::isLoaded);
    }

    public static final FanCatalystRotatingHeadBlock FAN_ENDING_CATALYST_DRAGON_HEAD = CCRegistrate.block("fan_ending_catalyst_dragon_head",
            properties -> new FanCatalystRotatingHeadBlock(properties, CCBlockEntityTypes.FAN_ENDING_CATALYST_DRAGON_HEAD),
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.TERRACOTTA_YELLOW).requiresCorrectToolForDrops().noOcclusion()
                    .lightLevel(s -> 0).isRedstoneConductor((state, level, pos) -> false));
    static {
        FeatureToggle.registerDependent(id("fan_ending_catalyst_dragon_head"), id("empty_fan_catalyst"));
        FeatureToggle.addCondition(id("fan_ending_catalyst_dragon_head"), Mods.DRAGONS_PLUS::isLoaded);
        CCRegistrate.blockItem(FAN_ENDING_CATALYST_DRAGON_HEAD, "fan_ending_catalyst_dragon_head");
    }

    public static final WrenchableBlock FAN_WITHERING_CATALYST = fanCatalyst("fan_withering_catalyst", 0);
    static {
        FeatureToggle.addCondition(id("fan_withering_catalyst"), () -> false); // No mods support bulk withering
    }
    public static final WrenchableBlock FAN_CHOCOLATE_COATING_CATALYST = fanCatalyst("fan_chocolate_coating_catalyst", 0);
    static {
        FeatureToggle.addCondition(id("fan_chocolate_coating_catalyst"), Mods.MORE_CATALYSTS::isLoaded);
    }
    public static final WrenchableBlock FAN_HONEY_COATING_CATALYST = fanCatalyst("fan_honey_coating_catalyst", 0);
    static {
        FeatureToggle.addCondition(id("fan_honey_coating_catalyst"), Mods.MORE_CATALYSTS::isLoaded);
    }

    public static final FanCatalystRotatingHeadBlock FAN_EXPLODING_CATALYST = CCRegistrate.block("fan_exploding_catalyst",
            properties -> new FanCatalystRotatingHeadBlock(properties, CCBlockEntityTypes.FAN_EXPLODING_CATALYST),
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.TERRACOTTA_YELLOW).requiresCorrectToolForDrops().noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false));
    static {
        FeatureToggle.registerDependent(id("fan_exploding_catalyst"), id("empty_fan_catalyst"));
        FeatureToggle.addCondition(id("fan_exploding_catalyst"), Mods.MORE_CATALYSTS::isLoaded);
        CCRegistrate.blockItem(FAN_EXPLODING_CATALYST, "fan_exploding_catalyst");
    }

    public static final WrenchableBlock FAN_RESONANCE_CATALYST = fanCatalyst("fan_resonance_catalyst", 3);
    static {
        FeatureToggle.addCondition(id("fan_resonance_catalyst"), Mods.MORE_CATALYSTS::isLoaded);
    }
    public static final WrenchableBlock FAN_SCULKING_CATALYST = fanCatalyst("fan_sculking_catalyst", 4);
    static {
        FeatureToggle.addCondition(id("fan_sculking_catalyst"), Mods.MORE_CATALYSTS::isLoaded);
    }
    public static final WrenchableBlock FAN_PURIFYING_CATALYST = fanCatalyst("fan_purifying_catalyst", 14);
    static {
        FeatureToggle.addCondition(id("fan_purifying_catalyst"), Mods.MORE_CATALYSTS::isLoaded);
    }
    public static final WrenchableBlock FAN_TRANSMUTATION_CATALYST = fanCatalyst("fan_transmutation_catalyst", 10);
    static {
        FeatureToggle.addCondition(id("fan_transmutation_catalyst"), Mods.SHIMMER::isLoaded);
    }
    public static final WrenchableBlock FAN_GLOOMING_CATALYST = fanCatalyst("fan_glooming_catalyst", 10);
    static {
        FeatureToggle.addCondition(id("fan_glooming_catalyst"), Mods.SHIMMER::isLoaded);
    }
    public static final WrenchableBlock FAN_SOUL_STRIPPING_CATALYST = fanCatalyst("fan_soul_stripping_catalyst", 0);
    static {
        FeatureToggle.addCondition(id("fan_soul_stripping_catalyst"), Mods.NETHER_INDUSTRY::isLoaded);
    }

    public static final Map<DyeColor, WrenchableBlock> FAN_DYEING_CATALYSTS = new TreeMap<>();

    static {
        for (DyeColor color : DyeColor.values()) {
            String namespace = DyeDepotCompat.getColorNamespace(color);
            boolean isVanilla = namespace.equals(Identifier.DEFAULT_NAMESPACE);
            String path = (isVanilla ? "" : (namespace + "_")) + color.getName() + "_fan_dyeing_catalyst";
            WrenchableBlock block = CCRegistrate.block(path, WrenchableBlock::new,
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.TERRACOTTA_YELLOW).requiresCorrectToolForDrops().noOcclusion()
                            .isRedstoneConductor((state, level, pos) -> false));
            FeatureToggle.registerDependent(id(path), id("empty_fan_catalyst"));
            FeatureToggle.addCondition(id(path), () -> (Mods.DRAGONS_PLUS.isLoaded() || Mods.GARNISHED.isLoaded()) && (isVanilla || Mods.DYE_DEPOT.isLoaded()));
            CCRegistrate.blockItem(block, path);
            FAN_DYEING_CATALYSTS.put(color, block);
        }
    }

    public static final ItemSiloBlock ITEM_SILO = CCRegistrate.block("item_silo", ItemSiloBlock::new,
            CCSharedProperties.softMetal().mapColor(MapColor.TERRACOTTA_BLUE).sound(SoundType.NETHERITE_BLOCK).explosionResistance(1200));
    static {
        FeatureToggle.register(id("item_silo"), FeatureCategory.LOGISTICS);
        MountedItemStorageType.REGISTRY.register(ITEM_SILO, CCMountedStorageTypes.SILO);
        BlockMovementChecks.registerAttachedCheck((state, world, pos, direction) -> {
            if (state.getBlock() instanceof ItemSiloBlock)
                return BlockMovementChecks.CheckResult.of(ConnectivityHandler.isConnected(world, pos, pos.relative(direction)));
            return BlockMovementChecks.CheckResult.PASS;
        });
        CCRegistrate.blockItem(ITEM_SILO, "item_silo", (b, p) -> new ItemSiloItem(b, p));
    }

    // .lightLevel(...) replaces the old Block.getLightEmission(state, world, pos) override point,
    // which no longer exists (see PORTING_NOTES.md session 14) - luminosity now lives on the
    // BlockState itself via FluidVesselBlock.LIGHT_LEVEL, kept in sync by
    // FluidVesselBlockEntity.updateStateLuminosity().
    public static final FluidVesselBlock FLUID_VESSEL = CCRegistrate.block("fluid_vessel", FluidVesselBlock::regular,
            CCSharedProperties.copperMetal().noOcclusion().isRedstoneConductor((p1, p2, p3) -> true)
                    .lightLevel(state -> state.getValue(FluidVesselBlock.LIGHT_LEVEL)));
    static {
        FeatureToggle.register(id("fluid_vessel"), FeatureCategory.LOGISTICS);
        BlockMovementChecks.registerAttachedCheck((state, world, pos, direction) -> {
            if (state.getBlock() instanceof FluidVesselBlock)
                return BlockMovementChecks.CheckResult.of(ConnectivityHandler.isConnected(world, pos, pos.relative(direction)));
            return BlockMovementChecks.CheckResult.PASS;
        });
        DisplaySource.BY_BLOCK.add(FLUID_VESSEL, CCDisplaySources.BOILER_STATUS);
        MountedFluidStorageType.REGISTRY.register(FLUID_VESSEL, CCMountedStorageTypes.FLUID_VESSEL);
        CCRegistrate.blockItem(FLUID_VESSEL, "fluid_vessel", (b, p) -> new FluidVesselItem(b, p));
    }

    public static final FluidVesselBlock CREATIVE_FLUID_VESSEL = CCRegistrate.block("creative_fluid_vessel", FluidVesselBlock::creative,
            CCSharedProperties.copperMetal().noOcclusion().mapColor(MapColor.COLOR_PURPLE)
                    .lightLevel(state -> state.getValue(FluidVesselBlock.LIGHT_LEVEL)));
    static {
        FeatureToggle.registerDependent(id("creative_fluid_vessel"), id("fluid_vessel"));
        CCRegistrate.blockItem(CREATIVE_FLUID_VESSEL, "creative_fluid_vessel", (b, p) -> new FluidVesselItem(b, p),
                new net.minecraft.world.item.Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC));
    }

    public static final InventoryAccessPortBlock INVENTORY_ACCESS_PORT = CCRegistrate.block("inventory_access_port", InventoryAccessPortBlock::new,
            CCSharedProperties.stone().mapColor(MapColor.TERRACOTTA_BROWN).noOcclusion());
    static {
        FeatureToggle.register(id("inventory_access_port"), FeatureCategory.LOGISTICS);
        CCRegistrate.blockItem(INVENTORY_ACCESS_PORT, "inventory_access_port");
    }

    public static final InventoryBridgeBlock INVENTORY_BRIDGE = CCRegistrate.block("inventory_bridge", InventoryBridgeBlock::new,
            CCSharedProperties.stone().mapColor(MapColor.TERRACOTTA_BROWN).noOcclusion());
    static {
        FeatureToggle.register(id("inventory_bridge"), FeatureCategory.LOGISTICS);
        CCRegistrate.blockItem(INVENTORY_BRIDGE, "inventory_bridge");
    }

    public static final BrassChuteBlock BRASS_CHUTE = CCRegistrate.block("brass_chute", BrassChuteBlock::new,
            CCSharedProperties.softMetal().mapColor(MapColor.TERRACOTTA_YELLOW).sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion().isSuffocating((state, level, pos) -> false));
    static {
        FeatureToggle.register(id("brass_chute"), FeatureCategory.LOGISTICS);
        CCRegistrate.blockItem(BRASS_CHUTE, "brass_chute", (b, p) -> new com.zurrtum.create.content.logistics.chute.ChuteItem(b, p));
    }

    public static final DashboardBlock DASHBOARD = CCRegistrate.block("dashboard", DashboardBlock::new,
            CCSharedProperties.stone().mapColor(MapColor.PODZOL));
    static {
        FeatureToggle.register(id("dashboard"), FeatureCategory.KINETIC);
        com.zurrtum.create.api.behaviour.display.DisplayTarget.BY_BLOCK.register(DASHBOARD, CCDisplayTargets.DASHBOARD);
        CCRegistrate.blockItem(DASHBOARD, "dashboard");
    }

    public static final CopycatSlabBlock COPYCAT_SLAB = CCRegistrate.block("copycat_slab", CopycatSlabBlock::new,
            CCBuilderTransformers.copycatProperties());
    static {
        FeatureToggle.register(id("copycat_slab"), FeatureCategory.COPYCATS);
        CCRegistrate.blockItem(COPYCAT_SLAB, "copycat_slab");
    }

    public static final CopycatBlockBlock COPYCAT_BLOCK = CCRegistrate.block("copycat_block", CopycatBlockBlock::new,
            CCBuilderTransformers.copycatProperties());
    static {
        FeatureToggle.register(id("copycat_block"), FeatureCategory.COPYCATS);
        CCRegistrate.blockItem(COPYCAT_BLOCK, "copycat_block");
    }

    public static final CopycatBeamBlock COPYCAT_BEAM = CCRegistrate.block("copycat_beam", CopycatBeamBlock::new,
            CCBuilderTransformers.copycatProperties());
    static {
        FeatureToggle.register(id("copycat_beam"), FeatureCategory.COPYCATS);
        CCRegistrate.blockItem(COPYCAT_BEAM, "copycat_beam");
    }

    public static final CopycatVerticalStepBlock COPYCAT_VERTICAL_STEP = CCRegistrate.block("copycat_vertical_step", CopycatVerticalStepBlock::new,
            CCBuilderTransformers.copycatProperties());
    static {
        FeatureToggle.register(id("copycat_vertical_step"), FeatureCategory.COPYCATS);
        CCRegistrate.blockItem(COPYCAT_VERTICAL_STEP, "copycat_vertical_step");
    }

    public static final CopycatStairsBlock COPYCAT_STAIRS = CCRegistrate.block("copycat_stairs", CopycatStairsBlock::new,
            CCBuilderTransformers.copycatProperties());
    static {
        FeatureToggle.register(id("copycat_stairs"), FeatureCategory.COPYCATS);
        CCRegistrate.blockItem(COPYCAT_STAIRS, "copycat_stairs");
    }

    public static final WrappedStairsBlock WRAPPED_COPYCAT_STAIRS = CCRegistrate.block("wrapped_copycat_stairs",
            p -> new WrappedStairsBlock(Blocks.STONE.defaultBlockState(), p), net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_STAIRS));
    static {
        CopycatStairsBlock.stairs = WRAPPED_COPYCAT_STAIRS;
    }

    public static final CopycatFenceBlock COPYCAT_FENCE = CCRegistrate.block("copycat_fence", CopycatFenceBlock::new,
            CCBuilderTransformers.copycatProperties());
    static {
        FeatureToggle.register(id("copycat_fence"), FeatureCategory.COPYCATS);
        CCRegistrate.blockItem(COPYCAT_FENCE, "copycat_fence");
    }

    public static final WrappedFenceBlock WRAPPED_COPYCAT_FENCE = CCRegistrate.block("wrapped_copycat_fence", WrappedFenceBlock::new,
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE));
    static {
        CopycatFenceBlock.fence = WRAPPED_COPYCAT_FENCE;
    }

    public static final CopycatWallBlock COPYCAT_WALL = CCRegistrate.block("copycat_wall", CopycatWallBlock::new,
            CCBuilderTransformers.copycatProperties().forceSolidOn());
    static {
        FeatureToggle.register(id("copycat_wall"), FeatureCategory.COPYCATS);
        CCRegistrate.blockItem(COPYCAT_WALL, "copycat_wall");
    }

    public static final WrappedWallBlock WRAPPED_COPYCAT_WALL = CCRegistrate.block("wrapped_copycat_wall", WrappedWallBlock::new,
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE_WALL));
    static {
        CopycatWallBlock.wall = WRAPPED_COPYCAT_WALL;
    }

    public static final CopycatFenceGateBlock COPYCAT_FENCE_GATE = CCRegistrate.block("copycat_fence_gate", CopycatFenceGateBlock::new,
            CCBuilderTransformers.copycatProperties().forceSolidOn());
    static {
        FeatureToggle.register(id("copycat_fence_gate"), FeatureCategory.COPYCATS);
        CCRegistrate.blockItem(COPYCAT_FENCE_GATE, "copycat_fence_gate");
    }

    public static final WrappedFenceGateBlock WRAPPED_COPYCAT_FENCE_GATE = CCRegistrate.block("wrapped_copycat_fence_gate",
            p -> new WrappedFenceGateBlock(net.minecraft.world.level.block.state.properties.WoodType.OAK, p), net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE));
    static {
        CopycatFenceGateBlock.fenceGate = WRAPPED_COPYCAT_FENCE_GATE;
    }

    public static final CopycatBoardBlock COPYCAT_BOARD = CCRegistrate.block("copycat_board", CopycatBoardBlock::new,
            CCBuilderTransformers.copycatProperties());
    static {
        FeatureToggle.register(id("copycat_board"), FeatureCategory.COPYCATS);
        CCRegistrate.blockItem(COPYCAT_BOARD, "copycat_board");
    }

    public static void register() {
        Mods.SIMULATED.executeIfInstalled(() -> SimCompatRegistry::register);
    }

    private static Identifier id(String path) {
        return com.hlysine.create_connected.CreateConnected.asResource(path);
    }
}
