package com.hlysine.create_connected.content.dashboard;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DashboardRenderer extends SmartBlockEntityRenderer<DashboardBlockEntity, DashboardRenderer.DashboardRenderState> {

    private static final int OUTLINE_RENDER_DISTANCE = Mth.square(16);

    private final Font font;

    public DashboardRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        this.font = context.font();
    }

    @Override
    public DashboardRenderState createRenderState() {
        return new DashboardRenderState();
    }

    @Override
    public void extractRenderState(
            DashboardBlockEntity be,
            DashboardRenderState state,
            float tickProgress,
            Vec3 cameraPos,
            @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        super.extractRenderState(be, state, tickProgress, cameraPos, crumblingOverlay);

        state.facing = be.getBlockState().getValue(DashboardBlock.FACING);
        state.signText = be.text;
        state.lineHeight = be.getTextLineHeight();
        state.midpoint = SignText.LINES * state.lineHeight / 2;

        state.messages = be.text.getRenderMessages(Minecraft.getInstance().isTextFilteringEnabled(), line -> {
            List<FormattedCharSequence> list = font.split(line, be.getMaxTextLineWidth());
            return list.isEmpty() ? FormattedCharSequence.EMPTY : list.get(0);
        });

        if (be.text.hasGlowingText()) {
            state.textColor = be.text.getColor().getTextColor();
            state.glowing = isOutlineVisible(be.getBlockPos(), state.textColor);
            state.light = 15728880;
        } else {
            state.textColor = SignRenderer.getDarkColor(be.text);
            state.glowing = false;
            state.light = state.lightCoords;
        }
    }

    @Override
    public void submit(DashboardRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        super.submit(state, matrices, queue, cameraState);

        matrices.pushPose();

        matrices.translate(0.5, 0.5, 0.5);
        matrices.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot()));
        matrices.translate(-0.5, -0.5, -0.5);

        matrices.translate(0.5, 12 / 16f, 9 / 16f);
        matrices.mulPose(Axis.XP.rotationDegrees(-66.80141f));
        matrices.translate(0, 3.5 / 16f, 0.15 / 16f);

        float scale = 0.015625f * 0.52f;
        matrices.scale(scale, -scale, scale);

        for (int i = 0; i < SignText.LINES; ++i) {
            FormattedCharSequence sequence = state.messages[i];
            float x = (float) (-font.width(sequence) / 2);
            float y = (float) (i * state.lineHeight - state.midpoint);
            int outlineColor = state.glowing ? SignRenderer.getDarkColor(state.signText) : 0;
            queue.submitText(
                    matrices,
                    x,
                    y,
                    sequence,
                    false,
                    Font.DisplayMode.POLYGON_OFFSET,
                    state.light,
                    state.textColor,
                    0,
                    outlineColor
            );
        }

        matrices.popPose();
    }

    private static boolean isOutlineVisible(BlockPos blockPos, int color) {
        if (color == DyeColor.BLACK.getTextColor()) {
            return true;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localPlayer = minecraft.player;
        if (localPlayer != null && minecraft.options.getCameraType().isFirstPerson() && localPlayer.isScoping()) {
            return true;
        }

        Entity entity = minecraft.getCameraEntity();
        return entity != null && entity.distanceToSqr(Vec3.atCenterOf(blockPos)) < (double) OUTLINE_RENDER_DISTANCE;
    }

    public static class DashboardRenderState extends SmartRenderState {
        public Direction facing = Direction.NORTH;
        public FormattedCharSequence[] messages = new FormattedCharSequence[SignText.LINES];
        public SignText signText = new SignText();
        public int lineHeight;
        public int midpoint;
        public int textColor;
        public boolean glowing;
        public int light;
    }
}
