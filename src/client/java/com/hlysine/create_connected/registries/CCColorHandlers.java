package com.hlysine.create_connected.registries;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.renderer.BiomeColors;

// IClientFluidTypeExtensions/FluidStack (NeoForge's fluid capability layer) don't exist on
// Fabric. Water's item-icon tint is a fixed vanilla constant (0x3F76E4), not derived from a
// fluid-capability lookup, so this simplifies rather than needing a real replacement API.
public class CCColorHandlers {

    public static BlockColor waterBlockTint() {
        return (state, level, pos, tintIndex) ->
                level != null && pos != null ? BiomeColors.getAverageWaterColor(level, pos) : -1;
    }

    private static final int WATER_TINT = 0x3F76E4;

    public static ItemColor waterItemTint() {
        return (stack, tintIndex) -> WATER_TINT;
    }
}
