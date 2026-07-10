package com.hlysine.create_connected.content.crankwheel;

import com.zurrtum.create.content.kinetics.base.IRotate;
import com.zurrtum.create.content.kinetics.crank.HandCrankBlockEntity;
import com.zurrtum.create.content.kinetics.simpleRelays.ICogWheel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

// CORRECTION: getRenderedHandle()/tickAudio() overrides used to live here (client-only
// AnimationTickHolder/CachedBuffers/SuperByteBuffer types), on the mistaken assumption that
// @OnlyIn(Dist.CLIENT)-marked methods with client-only imports were safe in a main-sourceset
// class - wrong under Loom's split source sets (compile-time classpath restriction, not runtime -
// see PORTING_NOTES.md). Investigated the real Create Fly architecture instead of just hooking
// around it: HandCrankBlockEntity has NO getRenderedHandle()/tickAudio() methods at all anymore -
// the "handle" render buffer is computed entirely client-side by
// com.zurrtum.create.client.content.kinetics.crank.HandCrankRenderer.getRenderedHandle(BlockState)
// (a renderer method, not a block-entity method), so the correct fix is a client-only
// CrankWheelRenderer extends HandCrankRenderer override (see src/client/java), not anything
// here. The cranking sound (tickAudio's AllSoundEvents.CRANKING) has no server-side equivalent
// method to override anymore either - Create Fly moved kinetic audio to a client-only
// KineticAudioBehaviour registered via BlockEntityBehaviour.CLIENT_REGISTRY - not yet
// re-implemented for this block (deferred, not silently dropped: flagged here and in
// PORTING_NOTES.md).
public class CrankWheelBlockEntity extends HandCrankBlockEntity {
    public CrankWheelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public List<BlockPos> addPropagationLocations(IRotate block, BlockState state, List<BlockPos> neighbours) {
        if (!ICogWheel.isLargeCog(state))
            return super.addPropagationLocations(block, state, neighbours);

        BlockPos.betweenClosedStream(new BlockPos(-1, -1, -1), new BlockPos(1, 1, 1))
                .forEach(offset -> {
                    if (offset.distSqr(BlockPos.ZERO) == 2)
                        neighbours.add(worldPosition.offset(offset));
                });
        return neighbours;
    }
}
