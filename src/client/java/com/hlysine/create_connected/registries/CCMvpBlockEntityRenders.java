package com.hlysine.create_connected.registries;

import com.hlysine.create_connected.content.crankwheel.CrankWheelRenderer;
import com.hlysine.create_connected.content.crankwheel.CrankWheelVisual;
import com.zurrtum.create.client.content.kinetics.simpleRelays.encased.EncasedCogRenderer;
import com.zurrtum.create.client.content.kinetics.simpleRelays.encased.EncasedCogVisual;
import com.zurrtum.create.client.content.kinetics.crank.HandCrankRenderer;
import com.zurrtum.create.client.content.kinetics.transmission.SplitShaftRenderer;
import com.zurrtum.create.client.content.kinetics.transmission.SplitShaftVisual;
import com.zurrtum.create.client.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import com.zurrtum.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.zurrtum.create.content.kinetics.crank.HandCrankBlockEntity;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class CCMvpBlockEntityRenders {
    private CCMvpBlockEntityRenders() {
    }

    public static void register() {
        BlockRenderLayerMap.putBlocks(
                ChunkSectionLayer.CUTOUT,
                CCBlocks.ENCASED_CHAIN_COGWHEEL,
                CCBlocks.CRANK_WHEEL,
                CCBlocks.LARGE_CRANK_WHEEL,
                CCBlocks.OVERSTRESS_CLUTCH,
                CCBlocks.INVERTED_CLUTCH,
                CCBlocks.INVERTED_GEARSHIFT,
                CCBlocks.CENTRIFUGAL_CLUTCH,
                CCBlocks.FREEWHEEL_CLUTCH,
                CCBlocks.BRAKE
        );

        BlockEntityRendererRegistry.register(CCBlockEntityTypes.ENCASED_CHAIN_COGWHEEL, EncasedCogRenderer::small);
        SimpleBlockEntityVisualizer.builder(CCBlockEntityTypes.ENCASED_CHAIN_COGWHEEL)
                .factory(EncasedCogVisual::small)
                .skipVanillaRender(be -> false)
                .apply();

        BlockEntityRendererProvider<HandCrankBlockEntity, HandCrankRenderer.HandCrankRenderState> crankRenderer = CrankWheelRenderer::new;
        BlockEntityRendererRegistry.register(CCBlockEntityTypes.CRANK_WHEEL, crankRenderer);
        SimpleBlockEntityVisualizer.builder(CCBlockEntityTypes.CRANK_WHEEL)
                .factory(CrankWheelVisual::new)
                .skipVanillaRender(be -> false)
                .apply();

        registerSplitShaft(CCBlockEntityTypes.OVERSTRESS_CLUTCH);
        registerSplitShaft(CCBlockEntityTypes.INVERTED_CLUTCH);
        registerSplitShaft(CCBlockEntityTypes.INVERTED_GEARSHIFT);
        registerSplitShaft(CCBlockEntityTypes.CENTRIFUGAL_CLUTCH);
        registerSplitShaft(CCBlockEntityTypes.FREEWHEEL_CLUTCH);
        registerSplitShaft(CCBlockEntityTypes.BRAKE);
    }

    private static <T extends SplitShaftBlockEntity> void registerSplitShaft(BlockEntityType<T> type) {
        BlockEntityRendererProvider<SplitShaftBlockEntity, SplitShaftRenderer.SplitShaftRenderState> renderer = SplitShaftRenderer::new;
        BlockEntityRendererRegistry.register(type, renderer);
        SimpleBlockEntityVisualizer.builder(type)
                .factory(SplitShaftVisual::new)
                .skipVanillaRender(be -> false)
                .apply();
    }
}
