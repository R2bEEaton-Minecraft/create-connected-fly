package com.hlysine.create_connected.registries;

import com.hlysine.create_connected.CreateConnected;
import com.hlysine.create_connected.content.fluidvessel.FluidVesselMountedStorageType;
import com.hlysine.create_connected.content.itemsilo.ItemSiloMountedStorageType;
import com.zurrtum.create.api.contraption.storage.fluid.MountedFluidStorageType;
import com.zurrtum.create.api.contraption.storage.item.MountedItemStorageType;
import com.zurrtum.create.api.registry.CreateRegistries;
import com.zurrtum.create.api.registry.CreateRegistryKeys;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class CCMountedStorageTypes {
    public static final ItemSiloMountedStorageType SILO = register("silo", new ItemSiloMountedStorageType());
    public static final FluidVesselMountedStorageType FLUID_VESSEL = register("fluid_vessel", new FluidVesselMountedStorageType());

    private static <T extends MountedItemStorageType<?>> T register(String id, T type) {
        return Registry.register(CreateRegistries.MOUNTED_ITEM_STORAGE_TYPE,
                ResourceKey.create(CreateRegistryKeys.MOUNTED_ITEM_STORAGE_TYPE, CreateConnected.asResource(id)), type);
    }

    private static <T extends MountedFluidStorageType<?>> T register(String id, T type) {
        return Registry.register(CreateRegistries.MOUNTED_FLUID_STORAGE_TYPE,
                ResourceKey.create(CreateRegistryKeys.MOUNTED_FLUID_STORAGE_TYPE, CreateConnected.asResource(id)), type);
    }

    public static void register() {
    }
}
