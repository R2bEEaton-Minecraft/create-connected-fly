package com.hlysine.create_connected.registries;

import com.hlysine.create_connected.CreateConnected;
import com.hlysine.create_connected.content.kineticbattery.KineticBatteryDisplaySource;
import com.zurrtum.create.api.behaviour.display.DisplaySource;
import com.zurrtum.create.api.registry.CreateRegistries;
import com.zurrtum.create.api.registry.CreateRegistryKeys;
import com.zurrtum.create.content.redstone.displayLink.source.BoilerDisplaySource;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class CCDisplaySources {
    public static final BoilerDisplaySource BOILER_STATUS = register("boiler_status", new BoilerDisplaySource());
    public static final KineticBatteryDisplaySource KINETIC_BATTERY = register("kinetic_battery", new KineticBatteryDisplaySource());

    private static <T extends DisplaySource> T register(String id, T source) {
        return Registry.register(CreateRegistries.DISPLAY_SOURCE,
                ResourceKey.create(CreateRegistryKeys.DISPLAY_SOURCE, CreateConnected.asResource(id)), source);
    }

    public static void register() {
    }
}
