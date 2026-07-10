package com.hlysine.create_connected.registries;

import com.hlysine.create_connected.CreateConnected;
import com.hlysine.create_connected.content.kineticbattery.KineticBatteryInteractionPoint;
import com.zurrtum.create.api.registry.CreateRegistries;
import com.zurrtum.create.api.registry.CreateRegistryKeys;
import com.zurrtum.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class CCArmInteractionPointTypes {
    public static final ArmInteractionPointType KINETIC_BATTERY = register("kinetic_battery", new KineticBatteryInteractionPoint.Type());

    private static <T extends ArmInteractionPointType> T register(String id, T type) {
        return Registry.register(CreateRegistries.ARM_INTERACTION_POINT_TYPE,
                ResourceKey.create(CreateRegistryKeys.ARM_INTERACTION_POINT_TYPE, CreateConnected.asResource(id)), type);
    }

    public static void register() {
    }
}
