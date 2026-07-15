package com.hlysine.create_connected.compat;

import com.hlysine.create_connected.CreateConnected;
import com.hlysine.create_connected.config.FeatureToggle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;

public record FeatureEnabledResourceCondition(Identifier feature) implements ResourceCondition {
    public static final MapCodec<FeatureEnabledResourceCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(Identifier.CODEC.fieldOf("tag").forGetter(FeatureEnabledResourceCondition::feature))
            .apply(instance, FeatureEnabledResourceCondition::new));

    public static final ResourceConditionType<FeatureEnabledResourceCondition> TYPE =
            ResourceConditionType.create(CreateConnected.asResource("feature_enabled"), CODEC);

    @Override
    public ResourceConditionType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean test(RegistryOps.RegistryInfoLookup registryInfo) {
        return FeatureToggle.isEnabled(feature);
    }
}
