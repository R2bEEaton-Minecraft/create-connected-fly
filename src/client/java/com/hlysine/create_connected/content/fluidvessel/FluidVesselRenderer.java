package com.hlysine.create_connected.content.fluidvessel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.catnip.animation.LerpedFloat;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.FluidRenderHelper;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class FluidVesselRenderer implements BlockEntityRenderer<FluidVesselBlockEntity, FluidVesselRenderer.FluidVesselRenderState> {
    public FluidVesselRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public FluidVesselRenderState createRenderState() {
        return new FluidVesselRenderState();
    }

    @Override
    public void extractRenderState(
            FluidVesselBlockEntity be,
            FluidVesselRenderState state,
            float tickProgress,
            Vec3 cameraPos,
            @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        if (!be.isController())
            return;

        if (be.hasWindow()) {
            updateFluidVesselState(be, state, tickProgress, crumblingOverlay);
        } else if (be.boiler.isActive()) {
            updateBoilerState(be, state, tickProgress, crumblingOverlay);
        }
    }

    private void updateFluidVesselState(
            FluidVesselBlockEntity be,
            FluidVesselRenderState state,
            float tickProgress,
            @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        LerpedFloat fluidLevel = be.getFluidLevel();
        if (fluidLevel == null)
            return;

        float capSize = 1 / 4f;
        float tankHullSize = 1 / 16f + 1 / 128f;
        float minPuddleHeight = 1 / 16f;
        float totalHeight = be.getWidth() - 2 * tankHullSize - minPuddleHeight;
        float level = fluidLevel.getValue(tickProgress);
        if (level < 1 / (512f * totalHeight))
            return;

        FluidStack fluidStack = be.getTankInventory().getFluid();
        if (fluidStack.isEmpty())
            return;

        BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
        state.layer = RenderTypes.translucentMovingBlock();
        FluidVesselRenderData data = new FluidVesselRenderData();
        state.data = data;

        float clampedLevel = Mth.clamp(level * totalHeight, 0, totalHeight);
        data.translateY = clampedLevel - totalHeight;
        data.light = state.lightCoords;
        data.fluid = fluidStack.getFluid();
        data.changes = fluidStack.getComponentChanges();

        Axis axis = be.getAxis();
        data.xMin = axis == Axis.X ? capSize : tankHullSize;
        data.xMax = axis == Axis.X
                ? data.xMin + be.getHeight() - 2 * capSize
                : data.xMin + be.getWidth() - 2 * tankHullSize;
        data.yMin = totalHeight + tankHullSize + minPuddleHeight - clampedLevel;
        data.yMax = data.yMin + clampedLevel;
        data.zMin = axis == Axis.Z ? capSize : tankHullSize;
        data.zMax = axis == Axis.Z
                ? data.zMin + be.getHeight() - 2 * capSize
                : data.zMin + be.getWidth() - 2 * tankHullSize;
    }

    private void updateBoilerState(
            FluidVesselBlockEntity be,
            FluidVesselRenderState state,
            float tickProgress,
            @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        boolean[] occludedDirections = be.boiler.occludedDirections;
        if (occludedDirections[0] && occludedDirections[1] && occludedDirections[2] && occludedDirections[3])
            return;

        BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
        state.layer = RenderTypes.cutoutMovingBlock();
        BoilerRenderData data = new BoilerRenderData();
        state.data = data;
        data.axis = be.getAxis();
        data.translateX = data.axis == Axis.X ? be.getHeight() / 2f : be.getWidth() / 2f;
        data.translateZ = data.axis == Axis.Z ? be.getHeight() / 2f : be.getWidth() / 2f;
        data.light = state.lightCoords;
        data.gaugeOffset = be.getWidth() / 2f - 6 / 16f;
        data.dialPivotY = 6f / 16;
        data.dialPivotZ = 8f / 16;
        data.progress = -145 * be.boiler.gauge.getValue(tickProgress) + 90;
        data.gauge = CachedBuffers.partial(AllPartialModels.BOILER_GAUGE, state.blockState);
        data.gaugeDial = CachedBuffers.partial(AllPartialModels.BOILER_GAUGE_DIAL, state.blockState);
        data.north = !occludedDirections[2] && data.axis == Axis.Z;
        data.south = !occludedDirections[0] && data.axis == Axis.Z;
        data.west = !occludedDirections[1] && data.axis == Axis.X;
        data.east = !occludedDirections[3] && data.axis == Axis.X;
    }

    @Override
    public void submit(
            FluidVesselRenderState state,
            PoseStack matrices,
            SubmitNodeCollector queue,
            CameraRenderState cameraState
    ) {
        if (state.data == null)
            return;

        state.data.translate(matrices);
        queue.submitCustomGeometry(matrices, state.layer, state.data);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    public static class FluidVesselRenderState extends BlockEntityRenderState {
        public RenderType layer;
        public RenderData data;
    }

    public interface RenderData extends SubmitNodeCollector.CustomGeometryRenderer {
        void translate(PoseStack matrices);
    }

    public static class FluidVesselRenderData implements RenderData {
        public Fluid fluid;
        public DataComponentPatch changes;
        public float xMin;
        public float xMax;
        public float yMin;
        public float yMax;
        public float zMin;
        public float zMax;
        public float translateY;
        public int light;

        @Override
        public void translate(PoseStack matrices) {
            matrices.translate(0, translateY, 0);
        }

        @Override
        public void render(PoseStack.Pose matricesEntry, VertexConsumer vertexConsumer) {
            FluidRenderHelper.renderFluidBox(
                    fluid,
                    changes,
                    xMin,
                    yMin,
                    zMin,
                    xMax,
                    yMax,
                    zMax,
                    vertexConsumer,
                    matricesEntry,
                    light,
                    false,
                    true
            );
        }
    }

    public static class BoilerRenderData implements RenderData {
        public Axis axis;
        public float translateX;
        public float translateZ;
        public float gaugeOffset;
        public float dialPivotY;
        public float dialPivotZ;
        public float progress;
        public SuperByteBuffer gauge;
        public SuperByteBuffer gaugeDial;
        public boolean north;
        public boolean south;
        public boolean west;
        public boolean east;
        public int light;

        @Override
        public void translate(PoseStack matrices) {
            matrices.translate(translateX, 0.5, translateZ);
        }

        private void renderFace(int yRot, PoseStack.Pose matricesEntry, VertexConsumer vertexConsumer) {
            gauge.rotateYDegrees(yRot)
                    .uncenter()
                    .translate(gaugeOffset, 0, 0)
                    .light(light)
                    .renderInto(matricesEntry, vertexConsumer);
            gaugeDial.rotateYDegrees(yRot)
                    .uncenter()
                    .translate(gaugeOffset, 0, 0)
                    .translate(0, dialPivotY, dialPivotZ)
                    .rotateXDegrees(progress)
                    .translate(0, -dialPivotY, -dialPivotZ)
                    .light(light)
                    .renderInto(matricesEntry, vertexConsumer);
        }

        @Override
        public void render(PoseStack.Pose matricesEntry, VertexConsumer vertexConsumer) {
            if (south)
                renderFace(-90, matricesEntry, vertexConsumer);
            if (west)
                renderFace(-180, matricesEntry, vertexConsumer);
            if (north)
                renderFace(-270, matricesEntry, vertexConsumer);
            if (east)
                renderFace(-360, matricesEntry, vertexConsumer);
        }
    }
}
