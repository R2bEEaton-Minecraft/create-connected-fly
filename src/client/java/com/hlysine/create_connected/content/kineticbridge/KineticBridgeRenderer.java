package com.hlysine.create_connected.content.kineticbridge;

import com.hlysine.create_connected.registries.CCPartialModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

// Rewritten for MC 1.21.11's new BlockEntityRenderer "render state" architecture - see
// BrassGearboxRenderer's comment and PORTING_NOTES.md "kinetic multi-shaft renderer rewrite" for
// the general shape of this change. The old `standardKineticRotationTransform(...)` helper this
// called doesn't exist anywhere (same class of stale pre-port leftover as `kineticRotationTransform`
// - confirmed absent from both this mod and the real Create Fly sources) - replaced with the same
// light()/rotateCentered() sequence the base class's own KineticRenderState.render() uses for its
// single default `model` field, applied here to the 2 buffers this renderer draws, reusing the
// base class's own precomputed `angle`/`direction` (kinetic rotation) render-state fields.
public class KineticBridgeRenderer extends KineticBlockEntityRenderer<KineticBlockEntity, KineticBridgeRenderer.KineticBridgeRenderState> {

    private final boolean isDestination;

    private KineticBridgeRenderer(BlockEntityRendererProvider.Context context, boolean isDestination) {
        super(context);
        this.isDestination = isDestination;
    }

    public static KineticBridgeRenderer source(BlockEntityRendererProvider.Context ctx) {
        return new KineticBridgeRenderer(ctx, false);
    }

    public static KineticBridgeRenderer destination(BlockEntityRendererProvider.Context ctx) {
        return new KineticBridgeRenderer(ctx, true);
    }

    @Override
    public KineticBridgeRenderState createRenderState() {
        return new KineticBridgeRenderState();
    }

    @Override
    public void extractRenderState(
            KineticBlockEntity be,
            KineticBridgeRenderState state,
            float tickProgress,
            Vec3 cameraPos,
            @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        Level level = be.getLevel();
        state.support = VisualizationManager.supportsVisualization(level);
        if (state.support) {
            return;
        }
        updateBaseRenderState(be, state, level, crumblingOverlay);

        Direction direction = be.getBlockState().getValue(KineticBridgeBlock.FACING);

        state.lightBehind = LevelRenderer.getLightColor(level, be.getBlockPos().relative(direction.getOpposite()));
        state.lightInFront = LevelRenderer.getLightColor(level, be.getBlockPos().relative(direction));

        state.shaftHalf = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), isDestination ? direction : direction.getOpposite());
        state.fanInner = CachedBuffers.partialFacing(isDestination ? CCPartialModels.KINETIC_BRIDGE_DESTINATION : CCPartialModels.KINETIC_BRIDGE_SOURCE, be.getBlockState(), isDestination ? direction : direction.getOpposite());
    }

    @Override
    protected RenderType getRenderType(KineticBlockEntity be, BlockState state) {
        return RenderTypes.cutoutMovingBlock();
    }

    public static class KineticBridgeRenderState extends KineticRenderState {
        public SuperByteBuffer shaftHalf;
        public SuperByteBuffer fanInner;
        public int lightBehind;
        public int lightInFront;

        @Override
        public void render(PoseStack.Pose matricesEntry, VertexConsumer vertexConsumer) {
            if (model != null) {
                super.render(matricesEntry, vertexConsumer);
            }
            shaftHalf.light(lightBehind);
            shaftHalf.rotateCentered(angle, direction);
            shaftHalf.renderInto(matricesEntry, vertexConsumer);

            fanInner.light(lightInFront);
            fanInner.rotateCentered(angle, direction);
            fanInner.renderInto(matricesEntry, vertexConsumer);
        }
    }
}
