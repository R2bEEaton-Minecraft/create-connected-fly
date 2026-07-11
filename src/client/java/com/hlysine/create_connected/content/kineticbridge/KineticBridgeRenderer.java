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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

public class KineticBridgeRenderer extends KineticBlockEntityRenderer<KineticBlockEntity> {

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
    protected void renderSafe(KineticBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) return;

        Direction direction = be.getBlockState().getValue(KineticBridgeBlock.FACING);
        VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());

        int lightBehind = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(direction.getOpposite()));
        int lightInFront = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(direction));

        SuperByteBuffer shaftHalf =
                CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), isDestination ? direction : direction.getOpposite());
        SuperByteBuffer fanInner =
                CachedBuffers.partialFacing(isDestination ? CCPartialModels.KINETIC_BRIDGE_DESTINATION : CCPartialModels.KINETIC_BRIDGE_SOURCE, be.getBlockState(), isDestination ? direction : direction.getOpposite());

        standardKineticRotationTransform(shaftHalf, be, lightBehind).renderInto(ms, vb);
        standardKineticRotationTransform(fanInner, be, lightInFront).renderInto(ms, vb);
    }
}

