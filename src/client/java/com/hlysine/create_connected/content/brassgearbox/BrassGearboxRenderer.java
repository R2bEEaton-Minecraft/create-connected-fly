package com.hlysine.create_connected.content.brassgearbox;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import com.zurrtum.create.client.catnip.animation.AnimationTickHolder;
import com.zurrtum.create.catnip.data.Iterate;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// Rewritten for MC 1.21.11's new BlockEntityRenderer "render state" architecture (real
// KineticBlockEntityRenderer<T, S> requires an extractRenderState(...)/render(...) split now
// instead of a single renderSafe(...) method - see PORTING_NOTES.md "kinetic multi-shaft renderer
// rewrite"). This renderer draws one shaft-half piece per non-axis direction (up to 4), so its
// RenderState holds a list of already-rotated pieces instead of the base class's single `model`
// field - same overall pattern as HandCrankRenderer/HandCrankRenderState (the reference this was
// modeled on), just with a list instead of one extra buffer.
public class BrassGearboxRenderer extends KineticBlockEntityRenderer<BrassGearboxBlockEntity, BrassGearboxRenderer.BrassGearboxRenderState> {

    public BrassGearboxRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public BrassGearboxRenderState createRenderState() {
        return new BrassGearboxRenderState();
    }

    @Override
    public void extractRenderState(
            BrassGearboxBlockEntity be,
            BrassGearboxRenderState state,
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
        final Axis boxAxis = be.getBlockState().getValue(BlockStateProperties.AXIS);
        final BlockPos pos = be.getBlockPos();
        float time = AnimationTickHolder.getRenderTime(level);

        for (Direction direction : Iterate.directions) {
            final Axis axis = direction.getAxis();
            if (boxAxis == axis)
                continue;

            SuperByteBuffer shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), direction);
            float offset = getRotationOffsetForPosition(be, pos, axis);
            float angle = (time * be.getSpeed() * 3f / 10) % 360;

            if (be.getSpeed() != 0 && be.hasSource()) {
                BlockPos source = be.source.subtract(be.getBlockPos());
                Direction sourceFacing = Direction.getNearest(source.getX(), source.getY(), source.getZ(), direction);
                angle *= BrassGearboxBlockEntity.getRotationSpeedModifier(direction, sourceFacing, be.getBlockState());
            }

            angle += offset;
            angle = angle / 180f * (float) Math.PI;

            state.shafts.add(new ShaftPiece(shaft, angle, direction));
        }
    }

    @Override
    protected RenderType getRenderType(BrassGearboxBlockEntity be, net.minecraft.world.level.block.state.BlockState state) {
        return RenderTypes.solidMovingBlock();
    }

    public record ShaftPiece(SuperByteBuffer buffer, float angle, Direction direction) {
    }

    public static class BrassGearboxRenderState extends KineticRenderState {
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
