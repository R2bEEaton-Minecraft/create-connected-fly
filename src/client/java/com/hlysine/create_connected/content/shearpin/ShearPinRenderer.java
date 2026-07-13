package com.hlysine.create_connected.content.shearpin;

import com.hlysine.create_connected.registries.CCPartialModels;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Renders the Shear Pin partial instead of Create's generic shaft model. */
public class ShearPinRenderer extends KineticBlockEntityRenderer<ShearPinBlockEntity, KineticBlockEntityRenderer.KineticRenderState> {
    public ShearPinRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(ShearPinBlockEntity blockEntity, KineticBlockEntityRenderer.KineticRenderState state) {
        return CachedBuffers.partial(CCPartialModels.SHEAR_PIN, blockEntity.getBlockState());
    }
}
