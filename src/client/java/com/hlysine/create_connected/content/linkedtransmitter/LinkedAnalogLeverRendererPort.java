package com.hlysine.create_connected.content.linkedtransmitter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.hlysine.create_connected.registries.CCBlocks;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.catnip.theme.Color;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.content.redstone.link.LinkRenderer;
import com.zurrtum.create.client.content.redstone.link.LinkRenderer.LinkRenderState;
import com.zurrtum.create.content.redstone.analogLever.AnalogLeverBlock;
import com.zurrtum.create.content.redstone.analogLever.AnalogLeverBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class LinkedAnalogLeverRendererPort implements BlockEntityRenderer<AnalogLeverBlockEntity, LinkedAnalogLeverRendererPort.LinkedAnalogLeverRenderState> {
    private final ItemModelResolver itemModelResolver;

    public LinkedAnalogLeverRendererPort(BlockEntityRendererProvider.Context context) {
        itemModelResolver = context.itemModelResolver();
    }

    @Override
    public LinkedAnalogLeverRenderState createRenderState() {
        return new LinkedAnalogLeverRenderState();
    }

    @Override
    public void extractRenderState(AnalogLeverBlockEntity be,
                                   LinkedAnalogLeverRenderState state,
                                   float tickProgress,
                                   Vec3 cameraPos,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(be, state, crumblingOverlay);
        state.layer = RenderTypes.solidMovingBlock();

        float level = be.clientState.getValue(tickProgress);
        state.angle = (float) ((level / 15) * 90 / 180 * Math.PI);
        state.handle = CachedBuffers.partial(AllPartialModels.ANALOG_LEVER_HANDLE, state.blockState);

        AttachFace face = state.blockState.getValue(AnalogLeverBlock.FACE);
        float rX = face == AttachFace.FLOOR ? 0 : face == AttachFace.WALL ? 90 : 180;
        float rY = AngleHelper.horizontalAngle(state.blockState.getValue(AnalogLeverBlock.FACING));
        state.xRot = Mth.DEG_TO_RAD * rX;
        state.yRot = Mth.DEG_TO_RAD * rY;

        state.indicator = CachedBuffers.partial(AllPartialModels.ANALOG_LEVER_INDICATOR, state.blockState);
        state.color = Color.mixColors(0x2C0300, 0xCD0000, level / 15f);

        if (state.blockState.is(CCBlocks.LINKED_ANALOG_LEVER) && be instanceof LinkedAnalogLeverBlockEntity linked) {
            double distance = be.isVirtual() ? -1 : cameraPos.distanceToSqr(Vec3.atCenterOf(be.getBlockPos()));
            state.link = LinkRenderer.getLinkRenderState(linked, itemModelResolver, distance);
        } else {
            state.link = null;
        }
    }

    @Override
    public void submit(LinkedAnalogLeverRenderState state,
                       PoseStack matrices,
                       SubmitNodeCollector queue,
                       CameraRenderState cameraState) {
        queue.submitCustomGeometry(matrices, state.layer, state);
        if (state.link != null) {
            state.link.render(state.blockState, queue, matrices, state.lightCoords);
        }
    }

    public static class LinkedAnalogLeverRenderState extends BlockEntityRenderState implements SubmitNodeCollector.CustomGeometryRenderer {
        public RenderType layer;
        public float angle;
        public SuperByteBuffer handle;
        public float xRot;
        public float yRot;
        public SuperByteBuffer indicator;
        public int color;
        public LinkRenderState link;

        @Override
        public void render(PoseStack.Pose matricesEntry, VertexConsumer vertexConsumer) {
            handle.rotateCentered(yRot, Direction.UP);
            handle.rotateCentered(xRot, Direction.EAST);
            handle.translate(0.5f, 0.0625f, 0.5f)
                    .rotate(angle, Direction.EAST)
                    .translate(-0.5f, -0.0625f, -0.5f);
            handle.light(lightCoords).renderInto(matricesEntry, vertexConsumer);

            indicator.rotateCentered(yRot, Direction.UP);
            indicator.rotateCentered(xRot, Direction.EAST);
            indicator.light(lightCoords).color(color).renderInto(matricesEntry, vertexConsumer);
        }
    }
}
