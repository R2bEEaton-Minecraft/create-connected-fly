package com.hlysine.create_connected.registries;

import com.hlysine.create_connected.content.crankwheel.CrankWheelRenderer;
import com.hlysine.create_connected.content.crankwheel.CrankWheelVisual;
import com.hlysine.create_connected.content.parallelgearbox.ParallelGearboxRenderer;
import com.hlysine.create_connected.content.parallelgearbox.ParallelGearboxVisual;
import com.hlysine.create_connected.content.sixwaygearbox.SixWayGearboxRenderer;
import com.hlysine.create_connected.content.sixwaygearbox.SixWayGearboxVisual;
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
import net.minecraft.world.level.block.Block;
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
                CCBlocks.BRAKE,
                CCBlocks.SIX_WAY_GEARBOX
        );
        // Railway's 1.21.11 conductor vent registers this explicitly in addition to its model's
        // render_type. This keeps the empty copycat indicator's transparent pixels out of SOLID.
        BlockRenderLayerMap.putBlocks(
                ChunkSectionLayer.CUTOUT,
                CCBlocks.COPYCAT_BLOCK,
                CCBlocks.COPYCAT_SLAB,
                CCBlocks.COPYCAT_BEAM,
                CCBlocks.COPYCAT_VERTICAL_STEP,
                CCBlocks.COPYCAT_STAIRS,
                CCBlocks.COPYCAT_FENCE,
                CCBlocks.COPYCAT_FENCE_GATE,
                CCBlocks.COPYCAT_WALL,
                CCBlocks.COPYCAT_BOARD
        );
        BlockRenderLayerMap.putBlocks(
                ChunkSectionLayer.CUTOUT,
                CCBlocks.EMPTY_FAN_CATALYST,
                CCBlocks.FAN_BLASTING_CATALYST,
                CCBlocks.FAN_SMOKING_CATALYST,
                CCBlocks.FAN_SPLASHING_CATALYST,
                CCBlocks.FAN_HAUNTING_CATALYST,
                CCBlocks.FAN_FREEZING_CATALYST,
                CCBlocks.FAN_SEETHING_CATALYST,
                CCBlocks.FAN_SANDING_CATALYST,
                CCBlocks.FAN_ENRICHED_CATALYST,
                CCBlocks.FAN_ENDING_CATALYST_DRAGONS_BREATH,
                CCBlocks.FAN_ENDING_CATALYST_DRAGON_HEAD,
                CCBlocks.FAN_WITHERING_CATALYST,
                CCBlocks.FAN_CHOCOLATE_COATING_CATALYST,
                CCBlocks.FAN_HONEY_COATING_CATALYST,
                CCBlocks.FAN_EXPLODING_CATALYST,
                CCBlocks.FAN_RESONANCE_CATALYST,
                CCBlocks.FAN_SCULKING_CATALYST,
                CCBlocks.FAN_PURIFYING_CATALYST,
                CCBlocks.FAN_TRANSMUTATION_CATALYST,
                CCBlocks.FAN_GLOOMING_CATALYST,
                CCBlocks.FAN_SOUL_STRIPPING_CATALYST
        );
        BlockRenderLayerMap.putBlocks(
                ChunkSectionLayer.CUTOUT,
                CCBlocks.FAN_DYEING_CATALYSTS.values().toArray(Block[]::new)
        );
        BlockRenderLayerMap.putBlocks(ChunkSectionLayer.TRANSLUCENT, CCBlocks.FAN_SPLASHING_CATALYST);

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

        BlockEntityRendererRegistry.register(CCBlockEntityTypes.PARALLEL_GEARBOX, ParallelGearboxRenderer::new);
        SimpleBlockEntityVisualizer.builder(CCBlockEntityTypes.PARALLEL_GEARBOX)
                .factory(ParallelGearboxVisual::new)
                .skipVanillaRender(be -> false)
                .apply();

        BlockEntityRendererRegistry.register(CCBlockEntityTypes.SIX_WAY_GEARBOX, SixWayGearboxRenderer::new);
        SimpleBlockEntityVisualizer.builder(CCBlockEntityTypes.SIX_WAY_GEARBOX)
                .factory(SixWayGearboxVisual::new)
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
