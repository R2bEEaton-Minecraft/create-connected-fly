package com.hlysine.create_connected.registries;

import com.hlysine.create_connected.CreateConnected;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;

import java.util.function.UnaryOperator;

public class CCDataComponents {
    public static final DataComponentType<Double> KINETIC_BATTERY_CHARGE = register(
            "kinetic_battery_charge",
            builder -> builder.persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE)
    );

    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
        ResourceKey<DataComponentType<?>> key = ResourceKey.create(Registries.DATA_COMPONENT_TYPE, CreateConnected.asResource(name));
        DataComponentType<T> type = builder.apply(DataComponentType.builder()).build();
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, key, type);
    }

    public static void register() {
    }
}
