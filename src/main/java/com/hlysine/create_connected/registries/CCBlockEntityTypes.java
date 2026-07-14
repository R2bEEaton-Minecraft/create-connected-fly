package com.hlysine.create_connected.registries;

import com.hlysine.create_connected.CreateConnected;
import com.hlysine.create_connected.content.brake.BrakeBlockEntity;
import com.hlysine.create_connected.content.brasschute.BrassChuteBlockEntity;
import com.hlysine.create_connected.content.brassgearbox.BrassGearboxBlockEntity;
import com.hlysine.create_connected.content.centrifugalclutch.CentrifugalClutchBlockEntity;
import com.hlysine.create_connected.content.crankwheel.CrankWheelBlockEntity;
import com.hlysine.create_connected.content.dashboard.DashboardBlockEntity;
import com.hlysine.create_connected.content.fancatalyst.FanCatalystRotatingHeadBlockEntity;
import com.hlysine.create_connected.content.fluidvessel.CreativeFluidVesselBlockEntity;
import com.hlysine.create_connected.content.fluidvessel.FluidVesselBlockEntity;
import com.hlysine.create_connected.content.freewheelclutch.FreewheelClutchBlockEntity;
import com.hlysine.create_connected.content.inventoryaccessport.InventoryAccessPortBlockEntity;
import com.hlysine.create_connected.content.inventorybridge.InventoryBridgeBlockEntity;
import com.hlysine.create_connected.content.invertedclutch.InvertedClutchBlockEntity;
import com.hlysine.create_connected.content.invertedgearshift.InvertedGearshiftBlockEntity;
import com.hlysine.create_connected.content.itemsilo.ItemSiloBlockEntity;
import com.hlysine.create_connected.content.kineticbattery.KineticBatteryBlockEntity;
import com.hlysine.create_connected.content.kineticbridge.KineticBridgeBlockEntity;
import com.hlysine.create_connected.content.kineticbridge.KineticBridgeDestinationBlockEntity;
import com.hlysine.create_connected.content.linkedtransmitter.LinkedAnalogLeverBlockEntity;
import com.hlysine.create_connected.content.linkedtransmitter.LinkedTransmitterBlockEntity;
import com.hlysine.create_connected.content.overstressclutch.OverstressClutchBlockEntity;
import com.hlysine.create_connected.content.parallelgearbox.ParallelGearboxBlockEntity;
import com.hlysine.create_connected.content.sequencedpulsegenerator.SequencedPulseGeneratorBlockEntity;
import com.hlysine.create_connected.content.shearpin.ShearPinBlockEntity;
import com.hlysine.create_connected.content.sixwaygearbox.SixWayGearboxBlockEntity;
import com.zurrtum.create.AllBlockEntityTypes;
import com.zurrtum.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.stream.Stream;

// Original was Registrate's .blockEntity(id, factory).visual(...).validBlocks(...).renderer(...).register().
// Registrate is gone (see PORTING_NOTES.md); this now does direct vanilla BlockEntityType registration.
// .visual()/.renderer() wiring is client-only (Flywheel visuals + BlockEntityRenderers) and is NOT
// re-created here yet - it needs its own client-sourceset class (e.g. CCBlockEntityRenderers under
// src/client/java), tracked as a follow-up in PORTING_NOTES.md. Without it the blocks will function
// but render as missing/no block entity renderer until that client class exists.
public class CCBlockEntityTypes {
    // Vanilla's BlockEntityType.Builder/BlockEntitySupplier are gone from the public API entirely
    // (BlockEntitySupplier is now package-private, confirmed via javap - Create Fly's own real
    // AllBlockEntityTypes.register() works around this by giving every block entity a 2-arg
    // (BlockPos, BlockState) constructor that hardcodes its own registered type via a static
    // self-reference, e.g. CopycatBlockEntity(pos, state) { super(AllBlockEntityTypes.COPYCAT, pos,
    // state); } - but this mod's own ~30 block entity classes all still use the previous 3-arg
    // (BlockEntityType<?>, BlockPos, BlockState) shape throughout their own code and renderers, so
    // rewriting every one of them wasn't worth it just to match this one registration helper.
    // Instead, this uses the standard "forward-reference holder" trick (well-established for exactly
    // this bootstrapping problem - the type doesn't exist yet when the factory needs to close over
    // it) to keep feeding a 3-arg XxxBlockEntity::new reference through Fabric's own
    // FabricBlockEntityTypeBuilder (net.fabricmc.fabric.api.object.builder.v1.block.entity, which
    // BlockEntityType now implements FabricBlockEntityType against) unchanged at every call site
    // below.
    @FunctionalInterface
    private interface Factory3<T extends net.minecraft.world.level.block.entity.BlockEntity> {
        T create(BlockEntityType<T> type, net.minecraft.core.BlockPos pos, BlockState state);
    }

    @SuppressWarnings("unchecked")
    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> register(
            String path, Factory3<T> factory, Block... validBlocks) {
        ResourceKey<BlockEntityType<?>> key = ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, CreateConnected.asResource(path));
        BlockEntityType<T>[] holder = new BlockEntityType[1];
        BlockEntityType<T> type = net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder
                .create((pos, state) -> factory.create(holder[0], pos, state), validBlocks)
                .build();
        holder[0] = type;
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, key, type);
    }

    @SuppressWarnings("rawtypes")
    private static void addSupported(BlockEntityType<?> type, Block... blocks) {
        net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType fabricType =
                (net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType) type;
        Stream.of(blocks).filter(java.util.Objects::nonNull).forEach(fabricType::addSupportedBlock);
    }

    public static final BlockEntityType<SimpleKineticBlockEntity> ENCASED_CHAIN_COGWHEEL =
            register("encased_chain_cogwheel", SimpleKineticBlockEntity::new, CCBlocks.ENCASED_CHAIN_COGWHEEL);

    public static final BlockEntityType<CrankWheelBlockEntity> CRANK_WHEEL =
            register("crank_wheel", CrankWheelBlockEntity::new, CCBlocks.CRANK_WHEEL, CCBlocks.LARGE_CRANK_WHEEL);

    public static final BlockEntityType<ParallelGearboxBlockEntity> PARALLEL_GEARBOX =
            register("parallel_gearbox", ParallelGearboxBlockEntity::new, CCBlocks.PARALLEL_GEARBOX);

    public static final BlockEntityType<SixWayGearboxBlockEntity> SIX_WAY_GEARBOX =
            register("six_way_gearbox", SixWayGearboxBlockEntity::new, CCBlocks.SIX_WAY_GEARBOX);

    public static final BlockEntityType<OverstressClutchBlockEntity> OVERSTRESS_CLUTCH =
            register("overstress_clutch", OverstressClutchBlockEntity::new, CCBlocks.OVERSTRESS_CLUTCH);

    public static final BlockEntityType<ShearPinBlockEntity> SHEAR_PIN =
            register("shear_pin", ShearPinBlockEntity::new, CCBlocks.SHEAR_PIN);

    public static final BlockEntityType<InvertedClutchBlockEntity> INVERTED_CLUTCH =
            register("inverted_clutch", InvertedClutchBlockEntity::new, CCBlocks.INVERTED_CLUTCH);

    public static final BlockEntityType<InvertedGearshiftBlockEntity> INVERTED_GEARSHIFT =
            register("inverted_gearshift", InvertedGearshiftBlockEntity::new, CCBlocks.INVERTED_GEARSHIFT);

    public static final BlockEntityType<CentrifugalClutchBlockEntity> CENTRIFUGAL_CLUTCH =
            register("centrifugal_clutch", CentrifugalClutchBlockEntity::new, CCBlocks.CENTRIFUGAL_CLUTCH);

    public static final BlockEntityType<FreewheelClutchBlockEntity> FREEWHEEL_CLUTCH =
            register("freewheel_clutch", FreewheelClutchBlockEntity::new, CCBlocks.FREEWHEEL_CLUTCH);

    public static final BlockEntityType<KineticBridgeBlockEntity> KINETIC_BRIDGE =
            register("kinetic_bridge", KineticBridgeBlockEntity::new, CCBlocks.KINETIC_BRIDGE);

    public static final BlockEntityType<KineticBridgeDestinationBlockEntity> KINETIC_BRIDGE_DESTINATION =
            register("kinetic_bridge_destination", KineticBridgeDestinationBlockEntity::new, CCBlocks.KINETIC_BRIDGE_DESTINATION);

    public static final BlockEntityType<BrassGearboxBlockEntity> BRASS_GEARBOX =
            register("brass_gearbox", BrassGearboxBlockEntity::new, CCBlocks.BRASS_GEARBOX);

    public static final BlockEntityType<BrakeBlockEntity> BRAKE =
            register("brake", BrakeBlockEntity::new, CCBlocks.BRAKE);

    public static final BlockEntityType<KineticBatteryBlockEntity> KINETIC_BATTERY =
            register("kinetic_battery", KineticBatteryBlockEntity::new, CCBlocks.KINETIC_BATTERY);

    public static final BlockEntityType<ItemSiloBlockEntity> ITEM_SILO =
            register("item_silo", ItemSiloBlockEntity::new, CCBlocks.ITEM_SILO);

    public static final BlockEntityType<FluidVesselBlockEntity> FLUID_VESSEL =
            register("fluid_vessel", FluidVesselBlockEntity::new, CCBlocks.FLUID_VESSEL);

    public static final BlockEntityType<CreativeFluidVesselBlockEntity> CREATIVE_FLUID_VESSEL =
            register("creative_fluid_vessel", CreativeFluidVesselBlockEntity::new, CCBlocks.CREATIVE_FLUID_VESSEL);

    public static final BlockEntityType<InventoryAccessPortBlockEntity> INVENTORY_ACCESS_PORT =
            register("inventory_access_port", InventoryAccessPortBlockEntity::new, CCBlocks.INVENTORY_ACCESS_PORT);

    public static final BlockEntityType<InventoryBridgeBlockEntity> INVENTORY_BRIDGE =
            register("inventory_bridge", InventoryBridgeBlockEntity::new, CCBlocks.INVENTORY_BRIDGE);

    public static final BlockEntityType<SequencedPulseGeneratorBlockEntity> SEQUENCED_PULSE_GENERATOR =
            register("sequenced_pulse_generator", SequencedPulseGeneratorBlockEntity::new, CCBlocks.SEQUENCED_PULSE_GENERATOR);

    public static final BlockEntityType<LinkedTransmitterBlockEntity> LINKED_TRANSMITTER = register("linked_transmitter",
            LinkedTransmitterBlockEntity::new,
            Stream.concat(Stream.of(CCBlocks.LINKED_LEVER), CCBlocks.LINKED_BUTTONS.values().stream()).toArray(Block[]::new));

    public static final BlockEntityType<LinkedAnalogLeverBlockEntity> LINKED_ANALOG_LEVER =
            register("linked_analog_lever", LinkedAnalogLeverBlockEntity::new, CCBlocks.LINKED_ANALOG_LEVER);

    public static final BlockEntityType<BrassChuteBlockEntity> BRASS_CHUTE =
            register("brass_chute", BrassChuteBlockEntity::new, CCBlocks.BRASS_CHUTE);

    public static final BlockEntityType<DashboardBlockEntity> DASHBOARD =
            register("dashboard", DashboardBlockEntity::new, CCBlocks.DASHBOARD);

    public static final BlockEntityType<FanCatalystRotatingHeadBlockEntity> FAN_ENDING_CATALYST_DRAGON_HEAD =
            register("fan_ending_catalyst_dragon_head", FanCatalystRotatingHeadBlockEntity::new, CCBlocks.FAN_ENDING_CATALYST_DRAGON_HEAD);

    public static final BlockEntityType<FanCatalystRotatingHeadBlockEntity> FAN_EXPLODING_CATALYST =
            register("fan_exploding_catalyst", FanCatalystRotatingHeadBlockEntity::new, CCBlocks.FAN_EXPLODING_CATALYST);

    public static void register() {
        // CCBlockEntityTypes can initialize while CCBlocks is still assigning its later fields.
        // Any null observed by a builder then produces an incomplete valid-block set, which modern
        // Minecraft rejects immediately on placement. Reconcile every Connected type here, after
        // both registry classes are fully initialized, instead of patching crashes one block at a
        // time. Re-adding an already-supported block is harmless.
        addSupported(ENCASED_CHAIN_COGWHEEL, CCBlocks.ENCASED_CHAIN_COGWHEEL);
        addSupported(CRANK_WHEEL, CCBlocks.CRANK_WHEEL, CCBlocks.LARGE_CRANK_WHEEL);
        addSupported(PARALLEL_GEARBOX, CCBlocks.PARALLEL_GEARBOX);
        addSupported(SIX_WAY_GEARBOX, CCBlocks.SIX_WAY_GEARBOX);
        addSupported(OVERSTRESS_CLUTCH, CCBlocks.OVERSTRESS_CLUTCH);
        addSupported(SHEAR_PIN, CCBlocks.SHEAR_PIN);
        addSupported(INVERTED_CLUTCH, CCBlocks.INVERTED_CLUTCH);
        addSupported(INVERTED_GEARSHIFT, CCBlocks.INVERTED_GEARSHIFT);
        addSupported(CENTRIFUGAL_CLUTCH, CCBlocks.CENTRIFUGAL_CLUTCH);
        addSupported(FREEWHEEL_CLUTCH, CCBlocks.FREEWHEEL_CLUTCH);
        addSupported(KINETIC_BRIDGE, CCBlocks.KINETIC_BRIDGE);
        addSupported(KINETIC_BRIDGE_DESTINATION, CCBlocks.KINETIC_BRIDGE_DESTINATION);
        addSupported(BRASS_GEARBOX, CCBlocks.BRASS_GEARBOX);
        addSupported(BRAKE, CCBlocks.BRAKE);
        addSupported(KINETIC_BATTERY, CCBlocks.KINETIC_BATTERY);
        addSupported(ITEM_SILO, CCBlocks.ITEM_SILO);
        addSupported(FLUID_VESSEL, CCBlocks.FLUID_VESSEL);
        addSupported(CREATIVE_FLUID_VESSEL, CCBlocks.CREATIVE_FLUID_VESSEL);
        addSupported(INVENTORY_ACCESS_PORT, CCBlocks.INVENTORY_ACCESS_PORT);
        addSupported(INVENTORY_BRIDGE, CCBlocks.INVENTORY_BRIDGE);
        addSupported(SEQUENCED_PULSE_GENERATOR, CCBlocks.SEQUENCED_PULSE_GENERATOR);
        addSupported(LINKED_TRANSMITTER,
                Stream.concat(Stream.of(CCBlocks.LINKED_LEVER), CCBlocks.LINKED_BUTTONS.values().stream())
                        .toArray(Block[]::new));
        addSupported(LINKED_ANALOG_LEVER, CCBlocks.LINKED_ANALOG_LEVER);
        addSupported(BRASS_CHUTE, CCBlocks.BRASS_CHUTE);
        addSupported(DASHBOARD, CCBlocks.DASHBOARD);
        addSupported(FAN_ENDING_CATALYST_DRAGON_HEAD, CCBlocks.FAN_ENDING_CATALYST_DRAGON_HEAD);
        addSupported(FAN_EXPLODING_CATALYST, CCBlocks.FAN_EXPLODING_CATALYST);

        // CopycatBlockEntity's two-argument constructor hardcodes Create's COPYCAT type. Registering a
        // second Connected type therefore produces an entity whose actual type rejects our block state
        // during placement. Fabric explicitly supports extending an existing type's valid-block set;
        // keep Create's implementation and teach it about every Connected copycat variant instead.
        net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType createCopycatType =
                (net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType) AllBlockEntityTypes.COPYCAT;
        Stream.of(CCBlocks.COPYCAT_BLOCK, CCBlocks.COPYCAT_SLAB, CCBlocks.COPYCAT_BEAM,
                        CCBlocks.COPYCAT_VERTICAL_STEP, CCBlocks.COPYCAT_STAIRS, CCBlocks.COPYCAT_FENCE,
                        CCBlocks.COPYCAT_FENCE_GATE, CCBlocks.COPYCAT_WALL, CCBlocks.COPYCAT_BOARD)
                .forEach(createCopycatType::addSupportedBlock);

        // AnalogLeverBlockEntity's constructor hardcodes Create's ANALOG_LEVER type before this mod's
        // subclass can retag itself to LINKED_ANALOG_LEVER. Let Create's own type accept the linked
        // block state during that early validation step, then the subclass can continue with its
        // Connected-specific behaviours.
        ((net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType) AllBlockEntityTypes.ANALOG_LEVER)
                .addSupportedBlock(CCBlocks.LINKED_ANALOG_LEVER);
    }
}
