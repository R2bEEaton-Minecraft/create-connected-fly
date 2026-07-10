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
import com.zurrtum.create.content.decoration.copycat.CopycatBlockEntity;
import com.zurrtum.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.stream.Stream;

// Original was Registrate's .blockEntity(id, factory).visual(...).validBlocks(...).renderer(...).register().
// Registrate is gone (see PORTING_NOTES.md); this now does direct vanilla BlockEntityType registration.
// .visual()/.renderer() wiring is client-only (Flywheel visuals + BlockEntityRenderers) and is NOT
// re-created here yet - it needs its own client-sourceset class (e.g. CCBlockEntityRenderers under
// src/client/java), tracked as a follow-up in PORTING_NOTES.md. Without it the blocks will function
// but render as missing/no block entity renderer until that client class exists.
public class CCBlockEntityTypes {
    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> register(
            String path, BlockEntityType.BlockEntitySupplier<T> factory, Block... validBlocks) {
        ResourceKey<BlockEntityType<?>> key = ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, CreateConnected.asResource(path));
        BlockEntityType<T> type = BlockEntityType.Builder.of(factory, validBlocks).build(null);
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, key, type);
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

    public static final BlockEntityType<CopycatBlockEntity> COPYCAT = register("copycat", CopycatBlockEntity::new,
            CCBlocks.COPYCAT_BLOCK, CCBlocks.COPYCAT_SLAB, CCBlocks.COPYCAT_BEAM, CCBlocks.COPYCAT_VERTICAL_STEP,
            CCBlocks.COPYCAT_STAIRS, CCBlocks.COPYCAT_FENCE, CCBlocks.COPYCAT_FENCE_GATE, CCBlocks.COPYCAT_WALL, CCBlocks.COPYCAT_BOARD);

    public static final BlockEntityType<FanCatalystRotatingHeadBlockEntity> FAN_ENDING_CATALYST_DRAGON_HEAD =
            register("fan_ending_catalyst_dragon_head", FanCatalystRotatingHeadBlockEntity::new, CCBlocks.FAN_ENDING_CATALYST_DRAGON_HEAD);

    public static final BlockEntityType<FanCatalystRotatingHeadBlockEntity> FAN_EXPLODING_CATALYST =
            register("fan_exploding_catalyst", FanCatalystRotatingHeadBlockEntity::new, CCBlocks.FAN_EXPLODING_CATALYST);

    public static void register() {
    }
}
