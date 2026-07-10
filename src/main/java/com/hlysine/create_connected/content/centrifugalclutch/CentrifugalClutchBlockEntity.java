package com.hlysine.create_connected.content.centrifugalclutch;

import com.hlysine.create_connected.registries.CCBlocks;
import com.zurrtum.create.content.kinetics.RotationPropagator;
import com.zurrtum.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerKineticScrollValueBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.ticks.TickPriority;

import java.util.List;

import static com.hlysine.create_connected.content.centrifugalclutch.CentrifugalClutchBlock.UNCOUPLED;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

public class CentrifugalClutchBlockEntity extends SplitShaftBlockEntity {

    public static final int DEFAULT_SPEED = 64;
    public static final int MAX_SPEED = 256;

    // Create Fly split the old unified ScrollValueBehaviour into a server-side data holder
    // (ServerKineticScrollValueBehaviour - value storage/sync/NBT, used here) and a client-only
    // wrapper (com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue
    // .ScrollValueBehaviour - UI board/slot hit-testing, constructed separately on the client
    // from this behaviour's ServerKineticScrollValueBehaviour.TYPE). This mod's custom client-side
    // "max/min speed" board row labels (previously in RotationScrollValueBehaviour.createBoard())
    // are not yet re-implemented - deferred pending research into where Create Fly now
    // instantiates the client-side value-box wrapper for a custom BlockEntity (see PORTING_NOTES.md).
    public ServerKineticScrollValueBehaviour speedThreshold;

    public boolean reattachNextTick = false;

    public CentrifugalClutchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        speedThreshold = new ServerKineticScrollValueBehaviour(this);
        speedThreshold.between(-MAX_SPEED, MAX_SPEED);
        speedThreshold.setValue(DEFAULT_SPEED);
        speedThreshold.withCallback(i -> this.onKineticUpdate());
        behaviours.add(speedThreshold);
    }

    @Override
    public void initialize() {
        onKineticUpdate();
        super.initialize();
    }

    private void onKineticUpdate() {
        boolean coupled = !getBlockState().getValue(UNCOUPLED);
        boolean thresholdReached = Mth.abs(getSpeed()) > 0;
        if (speedThreshold.getValue() < 0)
            thresholdReached = thresholdReached && Mth.abs(getSpeed()) <= Mth.abs(speedThreshold.getValue());
        else
            thresholdReached = thresholdReached && Mth.abs(getSpeed()) >= Mth.abs(speedThreshold.getValue());
        if (coupled != thresholdReached && !isOverStressed()) {
            if (level != null) {
                level.setBlockAndUpdate(getBlockPos(), getBlockState().cycle(UNCOUPLED));
                level.scheduleTick(getBlockPos(), CCBlocks.CENTRIFUGAL_CLUTCH, 0, TickPriority.EXTREMELY_HIGH);
                reattachNextTick = true;
            }
        }
    }

    @Override
    public void updateFromNetwork(float maxStress, float currentStress, int networkSize) {
        super.updateFromNetwork(maxStress, currentStress, networkSize);
        onKineticUpdate();
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        onKineticUpdate();
        super.onSpeedChanged(previousSpeed);
    }

    @Override
    public void tick() {
        super.tick();
        if (reattachNextTick && level != null) {
            reattachNextTick = false;
            RotationPropagator.handleAdded(level, getBlockPos(), this);
        }
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (face == getBlockState().getValue(FACING) && getBlockState().getValue(UNCOUPLED))
            return 0;
        return 1;
    }
}

