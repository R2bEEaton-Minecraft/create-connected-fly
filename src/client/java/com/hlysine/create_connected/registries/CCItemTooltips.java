package com.hlysine.create_connected.registries;

import com.hlysine.create_connected.CreateConnected;
import com.zurrtum.create.client.AllItemTooltips;
import net.minecraft.core.registries.BuiltInRegistries;

public final class CCItemTooltips {
    private CCItemTooltips() {
    }

    public static void register() {
        BuiltInRegistries.ITEM.stream()
                .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(CreateConnected.MODID))
                .forEach(AllItemTooltips::register);
    }
}
