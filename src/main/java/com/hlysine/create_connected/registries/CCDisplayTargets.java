package com.hlysine.create_connected.registries;

import com.hlysine.create_connected.CreateConnected;
import com.hlysine.create_connected.content.dashboard.DashboardDisplayTarget;
import com.zurrtum.create.api.behaviour.display.DisplayTarget;
import com.zurrtum.create.api.registry.CreateRegistries;
import com.zurrtum.create.api.registry.CreateRegistryKeys;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class CCDisplayTargets {
    public static final DashboardDisplayTarget DASHBOARD = register("dashboard", new DashboardDisplayTarget());

    private static <T extends DisplayTarget> T register(String id, T target) {
        return Registry.register(CreateRegistries.DISPLAY_TARGET,
                ResourceKey.create(CreateRegistryKeys.DISPLAY_TARGET, CreateConnected.asResource(id)), target);
    }

    public static void register() {
    }
}
