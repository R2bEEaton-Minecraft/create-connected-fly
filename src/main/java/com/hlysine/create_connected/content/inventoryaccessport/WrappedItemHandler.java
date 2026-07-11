package com.hlysine.create_connected.content.inventoryaccessport;

import net.minecraft.world.Container;

/**
 * Marker interface for all Container implementations that redirect calls to another Container
 * elsewhere (used to avoid infinite loops when following a chain of Inventory Bridges/Access Ports).
 * Real vanilla Container (already implements Create Fly's own BaseInventory, mixed in via
 * ContainerMixin) is the Fabric replacement for NeoForge's IItemHandler this used to extend.
 */
public interface WrappedItemHandler extends Container {
}
