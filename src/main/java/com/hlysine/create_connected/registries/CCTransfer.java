package com.hlysine.create_connected.registries;

import com.hlysine.create_connected.content.brasschute.BrassChuteBlockEntity;
import com.hlysine.create_connected.content.fluidvessel.FluidVesselBlockEntity;
import com.hlysine.create_connected.content.inventoryaccessport.InventoryAccessPortBlockEntity;
import com.hlysine.create_connected.content.inventorybridge.InventoryBridgeBlockEntity;
import com.hlysine.create_connected.content.itemsilo.ItemSiloBlockEntity;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.CachedFluidInventoryBehaviour;
import com.zurrtum.create.infrastructure.transfer.FluidInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;

/**
 * Real Create Fly's own capability registration ({@code com.zurrtum.create.AllTransfer}) exposes its
 * block entities' inventories onto Fabric's Transfer API via
 * {@code ItemStorage.SIDED.registerForBlockEntity(getter, type)} /
 * {@code FluidStorage.SIDED.registerForBlockEntity(getter, type)}. The item-side getter just needs to
 * return a plain vanilla {@link net.minecraft.world.Container}, wrapped via Fabric's own
 * {@code InventoryStorage.of(container, side)}; the fluid side wraps Create Fly's own
 * {@code FluidInventory} via its {@code FluidInventoryStorage.of(inventory, side)} (Fabric's Transfer
 * API has no vanilla-level fluid-container type to lean on, so Create Fly wrote its own adapter -
 * see {@code CachedInventoryBehaviour}/{@code CachedFluidInventoryBehaviour} in the real sources jar
 * for the reference patterns this mirrors, minus their extra per-block-entity caching-behaviour
 * indirection, which is a pure performance optimization, not something correctness depends on).
 * This replaces every {@code @SubscribeEvent RegisterCapabilitiesEvent}/
 * {@code Capabilities.ItemHandler.BLOCK}/{@code Capabilities.FluidHandler.BLOCK} call site this mod's
 * own ~15 capability-exposing block entities used to have under NeoForge.
 */
public class CCTransfer {
    public static void register() {
        ItemStorage.SIDED.registerForBlocks(
                (world, pos, state, blockEntity, side) -> blockEntity instanceof ItemSiloBlockEntity be
                        ? InventoryStorage.of(be.getItemCapability(), side)
                        : null,
                CCBlocks.ITEM_SILO
        );
        ItemStorage.SIDED.registerForBlocks(
                (world, pos, state, blockEntity, side) -> blockEntity instanceof InventoryAccessPortBlockEntity be
                        ? InventoryStorage.of(be.getItemCapability(), side)
                        : null,
                CCBlocks.INVENTORY_ACCESS_PORT
        );
        ItemStorage.SIDED.registerForBlocks(
                (world, pos, state, blockEntity, side) -> blockEntity instanceof InventoryBridgeBlockEntity be
                        ? InventoryStorage.of(be.getItemCapability(), side)
                        : null,
                CCBlocks.INVENTORY_BRIDGE
        );
        ItemStorage.SIDED.registerForBlocks(
                (world, pos, state, blockEntity, side) -> blockEntity instanceof BrassChuteBlockEntity be
                        ? InventoryStorage.of(be.itemHandler(), side)
                        : null,
                CCBlocks.BRASS_CHUTE
        );

        BlockEntityBehaviour.add(CCBlockEntityTypes.FLUID_VESSEL,
                be -> new CachedFluidInventoryBehaviour<>(be, tank -> {
                    if (tank.fluidCapability == null)
                        tank.refreshCapability();
                    return tank.fluidCapability;
                }));
        BlockEntityBehaviour.add(CCBlockEntityTypes.CREATIVE_FLUID_VESSEL,
                be -> new CachedFluidInventoryBehaviour<>(be, tank -> {
                    if (tank.fluidCapability == null)
                        tank.refreshCapability();
                    return tank.fluidCapability;
                }));
        FluidStorage.SIDED.registerForBlocks(
                (world, pos, state, blockEntity, side) -> blockEntity instanceof FluidVesselBlockEntity be
                        ? CachedFluidInventoryBehaviour.get(be, side)
                        : null,
                CCBlocks.FLUID_VESSEL
        );
        FluidStorage.SIDED.registerForBlocks(
                (world, pos, state, blockEntity, side) -> blockEntity instanceof FluidVesselBlockEntity be
                        ? CachedFluidInventoryBehaviour.get(be, side)
                        : null,
                CCBlocks.CREATIVE_FLUID_VESSEL
        );
    }
}
