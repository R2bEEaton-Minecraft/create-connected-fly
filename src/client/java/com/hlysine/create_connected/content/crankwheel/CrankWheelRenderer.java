package com.hlysine.create_connected.content.crankwheel;

import com.hlysine.create_connected.registries.CCPartialModels;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.content.kinetics.crank.HandCrankRenderer;
import com.zurrtum.create.content.kinetics.crank.HandCrankBlock;
import com.zurrtum.create.content.kinetics.simpleRelays.ICogWheel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

// Client-only fallback renderer for CrankWheelBlockEntity (used when Flywheel visualization is
// unsupported - see HandCrankRenderer.extractRenderState's shouldRenderShaft()/support check;
// CrankWheelVisual handles the normal Flywheel-backed path). Real Create Fly no longer has a
// getRenderedHandle()/tickAudio() pair on HandCrankBlockEntity itself (see
// CrankWheelBlockEntity.java's comment) - getRenderedHandle(BlockState) moved to be a plain
// instance method on the *renderer* (HandCrankRenderer), so this overrides that one method with
// this mod's large/small-cog handle model choice, exactly mirroring the original NeoForge
// CrankWheelBlockEntity.getRenderedHandle() body (recovered from the upstream NeoForge source at
// https://github.com/hlysine/create_connected for reference, then adapted to the new
// renderer-owns-this-method shape).
public class CrankWheelRenderer extends HandCrankRenderer {
    public CrankWheelRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public SuperByteBuffer getRenderedHandle(BlockState blockState) {
        Direction facing = blockState.getOptionalValue(HandCrankBlock.FACING).orElse(Direction.UP);
        boolean isLarge = ICogWheel.isLargeCog(blockState);
        return CachedBuffers.partialFacing(
                isLarge ? CCPartialModels.LARGE_CRANK_WHEEL_HANDLE : CCPartialModels.CRANK_WHEEL_HANDLE,
                blockState,
                facing.getOpposite()
        );
    }
}
