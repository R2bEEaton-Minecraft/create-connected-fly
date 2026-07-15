package com.hlysine.create_connected.compat;

import com.hlysine.create_connected.CreateConnected;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;

public record FeatureEnabledInCopycatsResourceCondition(Identifier feature) implements ResourceCondition {
    public static final MapCodec<FeatureEnabledInCopycatsResourceCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(Identifier.CODEC.fieldOf("tag").forGetter(FeatureEnabledInCopycatsResourceCondition::feature))
            .apply(instance, FeatureEnabledInCopycatsResourceCondition::new));

    public static final ResourceConditionType<FeatureEnabledInCopycatsResourceCondition> TYPE =
            ResourceConditionType.create(CreateConnected.asResource("feature_enabled_in_copycats"), CODEC);

    @Override
    public ResourceConditionType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean test(RegistryOps.RegistryInfoLookup registryInfo) {
        return Mods.COPYCATS.runIfInstalled(() -> () -> CopycatsManager.isFeatureEnabled(feature)).orElse(false);
    }
}
