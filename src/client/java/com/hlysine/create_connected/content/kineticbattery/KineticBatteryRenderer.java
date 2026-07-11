package com.hlysine.create_connected.content.kineticbattery;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.content.kinetics.base.IRotate;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;

import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import com.zurrtum.create.client.catnip.animation.AnimationTickHolder;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.catnip.data.Iterate;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// Rewritten for MC 1.21.11's new BlockEntityRenderer "render state" architecture - see
// BrassGearboxRenderer's comment and PORTING_NOTES.md "kinetic multi-shaft renderer rewrite" for
// the general shape of this change (same pattern, mechanically applied here).
public class KineticBatteryRenderer extends KineticBlockEntityRenderer<KineticBatteryBlockEntity, KineticBatteryRenderer.KineticBatteryRenderState> {

    public KineticBatteryRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public KineticBatteryRenderState createRenderState() {
        return new KineticBatteryRenderState();
    }

    @Override
    public void extractRenderState(
            KineticBatteryBlockEntity be,
            KineticBatteryRenderState state,
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

        state.shafts.clear();
        Block block = be.getBlockState().getBlock();
        final Axis boxAxis = ((IRotate) block).getRotationAxis(be.getBlockState());
        final BlockPos pos = be.getBlockPos();
        float time = AnimationTickHolder.getRenderTime(level);

        for (Direction direction : Iterate.directions) {
            Axis axis = direction.getAxis();
            if (boxAxis != axis)
                continue;

            float offset = getRotationOffsetForPosition(be, pos, axis);
            float angle = (time * be.getSpeed() * 3f / 10) % 360;
            float modifier = be.getRotationSpeedModifier(direction);

            angle *= modifier;
            angle += offset;
            angle = angle / 180f * (float) Math.PI;

            SuperByteBuffer superByteBuffer =
                    CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), direction);
            state.shafts.add(new ShaftPiece(superByteBuffer, angle, direction));
        }
    }

    @Override
    protected RenderType getRenderType(KineticBatteryBlockEntity be, BlockState state) {
        return RenderTypes.solidMovingBlock();
    }

    public record ShaftPiece(SuperByteBuffer buffer, float angle, Direction direction) {
    }

    public static class KineticBatteryRenderState extends KineticRenderState {
        public final List<ShaftPiece> shafts = new ArrayList<>();

        @Override
        public void render(PoseStack.Pose matricesEntry, VertexConsumer vertexConsumer) {
            if (model != null) {
                super.render(matricesEntry, vertexConsumer);
            }
            for (ShaftPiece piece : shafts) {
                piece.buffer.light(lightCoords);
                piece.buffer.rotateCentered(piece.angle, piece.direction);
                piece.buffer.color(color);
                piece.buffer.renderInto(matricesEntry, vertexConsumer);
            }
        }
    }

}
