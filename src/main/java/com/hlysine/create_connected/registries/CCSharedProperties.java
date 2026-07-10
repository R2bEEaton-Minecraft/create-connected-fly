package com.hlysine.create_connected.registries;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

// Reconstruction of the original com.simibubi.create.foundation.data.SharedProperties helper,
// which does not exist in Create Fly (the whole Registrate-backed foundation.data package is
// gone). Exact hardness/sound values are a reasonable approximation, not verified against the
// original NeoForge Create source; only affects mining feel, not correctness of behavior.
public class CCSharedProperties {
    public static BlockBehaviour.Properties stone() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .requiresCorrectToolForDrops()
                .strength(1.25f, 4.5f)
                .sound(SoundType.STONE);
    }

    public static BlockBehaviour.Properties wooden() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.0f, 3.0f)
                .sound(SoundType.WOOD)
                .ignitedByLava();
    }

    public static BlockBehaviour.Properties softMetal() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .requiresCorrectToolForDrops()
                .strength(3.0f, 6.0f)
                .sound(SoundType.NETHERITE_BLOCK);
    }

    public static BlockBehaviour.Properties copperMetal() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .requiresCorrectToolForDrops()
                .strength(3.0f, 6.0f)
                .sound(SoundType.COPPER);
    }
}
