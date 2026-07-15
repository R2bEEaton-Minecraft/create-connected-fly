package com.hlysine.create_connected.compat;

import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;

public final class CCResourceConditions {
    private static boolean registered;

    private CCResourceConditions() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        ResourceConditions.register(FeatureEnabledResourceCondition.TYPE);
        ResourceConditions.register(FeatureEnabledInCopycatsResourceCondition.TYPE);
        registered = true;
    }
}
