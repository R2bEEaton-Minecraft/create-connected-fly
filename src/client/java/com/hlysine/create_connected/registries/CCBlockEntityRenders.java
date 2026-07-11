package com.hlysine.create_connected.registries;

import com.hlysine.create_connected.content.crankwheel.CrankWheelRenderer;
import com.hlysine.create_connected.content.crankwheel.CrankWheelVisual;
import com.zurrtum.create.content.kinetics.crank.HandCrankBlockEntity;
import com.zurrtum.create.client.content.kinetics.crank.HandCrankRenderer;
import com.zurrtum.create.client.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

// Client-only block entity renderer/visualizer registration. Mirrors the real Create Fly
// AllBlockEntityRenders.normal(...) pattern (BlockEntityRenderers.register + a
// SimpleBlockEntityVisualizer with skipVanillaRender=false, i.e. keep the vanilla-renderer
// fallback path alive for when Flywheel visualization isn't supported) - see
// com.zurrtum.create.client.AllBlockEntityRenders for the reference this was copied from.
// Only CrankWheel is wired here for now; other block entities in this mod don't yet have
// their client renderer/visual split finished (see PORTING_NOTES.md).
public class CCBlockEntityRenders {
    public static void register() {
        BlockEntityRendererProvider<HandCrankBlockEntity, HandCrankRenderer.HandCrankRenderState> crankWheelRendererFactory = CrankWheelRenderer::new;
        BlockEntityRendererRegistry.register(CCBlockEntityTypes.CRANK_WHEEL, crankWheelRendererFactory);
        SimpleBlockEntityVisualizer.builder(CCBlockEntityTypes.CRANK_WHEEL)
                .factory(CrankWheelVisual::new)
                .skipVanillaRender(be -> false)
                .apply();
    }
}
