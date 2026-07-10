package com.hlysine.create_connected.content.kineticbattery;

import com.hlysine.create_connected.CreateConnected;
import com.hlysine.create_connected.registries.CCDataComponents;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.Identifier;

// addOverrideModels() (RegistrateItemModelProvider datagen for the override models per charge
// level) dropped - those item model JSON files are already static under
// src/main/resources/assets/create_connected/models/item/kinetic_battery_level_*.json (or
// wherever they ended up) per the established "assets are static, drop datagen" rule.
public class KineticBatteryOverrides {

    public static final Identifier ID = CreateConnected.asResource("kinetic_battery_level");

    public static void registerModelOverridesClient(KineticBatteryBlockItem item) {
        ItemProperties.register(item, ID, (pStack, pLevel, pEntity, pSeed) -> {
            double level = pStack.getOrDefault(CCDataComponents.KINETIC_BATTERY_CHARGE, 0.0);
            return KineticBatteryBlockEntity.getCrudeBatteryLevel(level, 5);
        });
    }
}

