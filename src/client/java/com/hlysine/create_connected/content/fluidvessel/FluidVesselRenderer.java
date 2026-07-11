package com.hlysine.create_connected.content.fluidvessel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import com.zurrtum.create.catnip.animation.LerpedFloat;
import com.zurrtum.create.catnip.data.Iterate;
import com.zurrtum.create.catnip.platform.NeoForgeCatnipServices;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

// Rewritten onto MC 1.21.11's new BlockEntityRenderer "render state" architecture (real
// SafeBlockEntityRenderer was renamed to SmartBlockEntityRenderer<T, S> - verified via the real
// sources jar and javap on the resolved jar; see PORTING_NOTES.md "FluidVesselRenderer /
// KineticBridgeRenderer render-state rewrite"). Unlike KineticBlockEntityRenderer's
// CustomGeometryRenderer-per-state pattern, SmartBlockEntityRenderer draws directly in submit(...)
// (which receives a real PoseStack), so extractRenderState just captures the data needed
// (fluid/boiler state, dimensions) and submit() does the actual pushPose/translate/draw calls -
// same overall idea, different real API shape for this base class.
//
// NOT YET FIXED, deferred (not stubbed): the fluid-box rendering below still references
// NeoForge's own FluidStack/FluidTank/NeoForgeCatnipServices.FLUID_RENDERER, none of which exist
// on Fabric - this is the same NeoForge-Capabilities-to-Fabric-Transfer-API rewrite already tracked
// as its own separate priority item (the ~15 capability block entities), not something to
// improvise here. The boiler-gauge rendering (no fluid capability dependency at all) is fully
// fixed onto the new architecture below.
public class FluidVesselRenderer extends SmartBlockEntityRenderer<FluidVesselBlockEntity, FluidVesselRenderer.FluidVesselRenderState> {

    public FluidVesselRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
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
        super.extractRenderState(be, state, tickProgress, cameraPos, crumblingOverlay);
        state.isController = be.isController();
        if (!state.isController)
            return;

        state.hasWindow = be.hasWindow();
        state.boilerActive = be.boiler.isActive();
        state.axis = be.getAxis();
        state.width = be.getWidth();
        state.height = be.getHeight();

        if (!state.hasWindow) {
            if (state.boilerActive) {
                state.gaugeProgress = be.boiler.gauge.getValue(tickProgress);
                state.occludedDirections = be.boiler.occludedDirections.clone();
            }
            return;
        }

        LerpedFloat fluidLevel = be.getFluidLevel();
        if (fluidLevel == null) {
            state.fluidLevelValue = -1;
            return;
        }
        state.fluidLevelValue = fluidLevel.getValue(tickProgress);
        state.tankInventory = be.getTankInventory();
    }

    @Override
    public void submit(FluidVesselRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        super.submit(state, matrices, queue, cameraState);
        if (!state.isController)
            return;

        if (!state.hasWindow) {
            if (state.boilerActive)
                submitBoiler(state, matrices, queue);
            return;
        }

        if (state.fluidLevelValue < 0)
            return;

        float capSize = 1 / 4f;
        float tankHullSize = 1 / 16f + 1 / 128f;
        float minPuddleHeight = 1 / 16f;
        float totalHeight = state.width - 2 * tankHullSize - minPuddleHeight;

        float level = state.fluidLevelValue;
        if (level < 1 / (512f * totalHeight))
            return;
        float clampedLevel = Mth.clamp(level * totalHeight, 0, totalHeight);

        FluidTank tank = state.tankInventory;
        FluidStack fluidStack = tank.getFluid();

        if (fluidStack.isEmpty())
            return;

        boolean top = fluidStack.getFluid()
                .getFluidType()
                .isLighterThanAir();

        Axis axis = state.axis;
        float xMin = axis == Axis.X ? capSize : tankHullSize;
        float xMax = axis == Axis.X ? xMin + state.height - 2 * capSize : xMin + state.width - 2 * tankHullSize;
        float yMin = totalHeight + tankHullSize + minPuddleHeight - clampedLevel;
        float yMax = yMin + clampedLevel;

        if (top) {
            yMin += totalHeight - clampedLevel;
            yMax += totalHeight - clampedLevel;
        }

        float zMin = axis == Axis.Z ? capSize : tankHullSize;
        float zMax = axis == Axis.Z ? zMin + state.height - 2 * capSize : zMin + state.width - 2 * tankHullSize;

        MultiBufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        matrices.pushPose();
        matrices.translate(0, clampedLevel - totalHeight, 0);
        NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(
                fluidStack,
                xMin, yMin, zMin,
                xMax, yMax, zMax,
                buffer,
                matrices,
                state.lightCoords,
                false,
                true
        );
        matrices.popPose();
    }

    protected void submitBoiler(FluidVesselRenderState state, PoseStack matrices, SubmitNodeCollector queue) {
        BlockState blockState = state.blockState;
        matrices.pushPose();
        var msr = TransformStack.of(matrices);
        Axis axis = state.axis;
        msr.translate(axis == Axis.X ? state.height / 2f : state.width / 2f, 0.5, axis == Axis.Z ? state.height / 2f : state.width / 2f);

        float dialPivotY = 6f / 16;
        float dialPivotZ = 8f / 16;
        float progress = state.gaugeProgress;
        int light = state.lightCoords;

        queue.submitCustomGeometry(matrices, RenderTypes.cutoutMovingBlock(), (matricesEntry, vertexConsumer) -> {
            for (Direction d : Iterate.horizontalDirections) {
                if (state.occludedDirections[d.get2DDataValue()])
                    continue;
                if (d.getAxis() != axis)
                    continue;
                float yRot = -d.toYRot() - 90;
                CachedBuffers.partial(AllPartialModels.BOILER_GAUGE, blockState)
                        .rotateYDegrees(yRot)
                        .uncenter()
                        .translate(state.width / 2f - 6 / 16f, 0, 0)
                        .light(light)
                        .renderInto(matricesEntry, vertexConsumer);
                CachedBuffers.partial(AllPartialModels.BOILER_GAUGE_DIAL, blockState)
                        .rotateYDegrees(yRot)
                        .uncenter()
                        .translate(state.width / 2f - 6 / 16f, 0, 0)
                        .translate(0, dialPivotY, dialPivotZ)
                        .rotateXDegrees(-145 * progress + 90)
                        .translate(0, -dialPivotY, -dialPivotZ)
                        .light(light)
                        .renderInto(matricesEntry, vertexConsumer);
            }
        });

        matrices.popPose();
    }

    // Real, minor behavior change: vanilla's shouldRenderOffScreen() no longer takes the
    // BlockEntity instance (see PORTING_NOTES.md), so we can no longer return `true` only for the
    // controller part of a multi-block vessel and `false` otherwise - always returning `true` here
    // errs on the side of the old controller behavior (always rendered regardless of view
    // frustum), at the cost of slightly less strict culling for non-controller parts, which is a
    // negligible performance difference, not a functional regression.
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    public static class FluidVesselRenderState extends SmartRenderState {
        public boolean isController;
        public boolean hasWindow;
        public boolean boilerActive;
        public Axis axis;
        public int width;
        public int height;
        public float gaugeProgress;
        public boolean[] occludedDirections;
        public float fluidLevelValue = -1;
        public FluidTank tankInventory;
    }
}
