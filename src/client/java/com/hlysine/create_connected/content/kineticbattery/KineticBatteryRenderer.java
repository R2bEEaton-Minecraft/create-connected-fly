package com.hlysine.create_connected.content.kineticbattery;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.content.kinetics.base.IRotate;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;

import com.zurrtum.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import com.zurrtum.create.client.catnip.animation.AnimationTickHolder;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.catnip.data.Iterate;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;

public class KineticBatteryRenderer extends KineticBlockEntityRenderer<KineticBatteryBlockEntity> {

    public KineticBatteryRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(KineticBatteryBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) return;

        Block block = be.getBlockState().getBlock();
        final Axis boxAxis = ((IRotate) block).getRotationAxis(be.getBlockState());
        final BlockPos pos = be.getBlockPos();
        float time = AnimationTickHolder.getRenderTime(be.getLevel());

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
            kineticRotationTransform(superByteBuffer, be, axis, angle, light);
            superByteBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()));
        }
    }

}

