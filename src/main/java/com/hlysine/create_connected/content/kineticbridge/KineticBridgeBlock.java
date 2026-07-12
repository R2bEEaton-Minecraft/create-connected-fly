package com.hlysine.create_connected.content.kineticbridge;

import com.hlysine.create_connected.registries.CCBlockEntityTypes;
import com.hlysine.create_connected.registries.CCBlocks;
import com.zurrtum.create.content.kinetics.base.DirectionalKineticBlock;
import com.zurrtum.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class KineticBridgeBlock extends DirectionalKineticBlock implements IBE<KineticBridgeBlockEntity> {

    public KineticBridgeBlock(Properties properties) {
        super(properties);
    }

    private BlockState getBaseStateForPlacement(BlockPlaceContext context) {
        Direction preferred = getPreferredFacing(context);
        if (preferred == null || (context.getPlayer() != null && context.getPlayer()
                .isShiftKeyDown())) {
            Direction nearestLookingDirection = context.getNearestLookingDirection();
            return defaultBlockState().setValue(FACING, context.getPlayer() != null && context.getPlayer()
                    .isShiftKeyDown() ? nearestLookingDirection.getOpposite() : nearestLookingDirection);
        }
        return defaultBlockState().setValue(FACING, preferred.getOpposite());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState stateForPlacement = getBaseStateForPlacement(context);
        BlockPos pos = context.getClickedPos();
        Direction facing = stateForPlacement.getValue(FACING);

        BlockPos destinationPos = pos.relative(facing);
        BlockState occupiedState = context.getLevel().getBlockState(destinationPos);
        if (!occupiedState.canBeReplaced())
            return null;

        return stateForPlacement;
    }

    public Direction getDirectionForPlacement(BlockPlaceContext context) {
        return getBaseStateForPlacement(context).getValue(FACING);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.getBlockTicks().hasScheduledTick(pos, this))
            level.scheduleTick(pos, this, 1);
    }

    // BlockBehaviour.onRemove(BlockState, Level, BlockPos, BlockState newState, boolean) was replaced by
    // affectNeighborsAfterRemoval(BlockState, ServerLevel, BlockPos, boolean movedByPiston) (confirmed
    // via javap; see MigratingCopycatBlock.java for the fuller writeup). Unlike the Copycats+-only
    // overrides there, this override's `!pNewState.is(this)` guard was real, load-bearing logic (only
    // tear down the paired destination block when this block is actually being removed, not merely
    // transitioning to a different FACING/state of itself). The newState param is simply gone now, but
    // the guard is no longer necessary to reproduce: the new method's own name/contract (and vanilla's
    // own call site, which now filters same-block state transitions before invoking it at all) means
    // affectNeighborsAfterRemoval is only invoked on genuine removal in the first place, so the check is
    // redundant rather than unexpressable - dropped rather than replaced.
    @Override
    protected void affectNeighborsAfterRemoval(BlockState pState, ServerLevel pLevel, BlockPos pPos, boolean movedByPiston) {
        Direction facing = pState.getValue(FACING);
        BlockPos destinationPos = pPos.relative(facing);

        BlockState occupiedState = pLevel.getBlockState(destinationPos);
        BlockState requiredStructure = CCBlocks.KINETIC_BRIDGE_DESTINATION.defaultBlockState()
                .setValue(KineticBridgeDestinationBlock.FACING, facing);
        if (occupiedState.equals(requiredStructure)) {
            pLevel.destroyBlock(destinationPos, false);
        }
        super.affectNeighborsAfterRemoval(pState, pLevel, pPos, movedByPiston);
    }

    @Override
    public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        Direction facing = pState.getValue(FACING);
        BlockPos destinationPos = pPos.relative(facing);

        BlockState occupiedState = pLevel.getBlockState(destinationPos);
        BlockState requiredStructure = CCBlocks.KINETIC_BRIDGE_DESTINATION.defaultBlockState()
                .setValue(KineticBridgeDestinationBlock.FACING, facing);
        if (!occupiedState.equals(requiredStructure)) {
            if (!occupiedState.canBeReplaced()) {
                pLevel.destroyBlock(pPos, true);
                return;
            }
            pLevel.setBlockAndUpdate(destinationPos, requiredStructure);
        }
    }

    @Override
    public BlockEntityType<? extends KineticBridgeBlockEntity> getBlockEntityType() {
        return CCBlockEntityTypes.KINETIC_BRIDGE;
    }

    @Override
    public Class<KineticBridgeBlockEntity> getBlockEntityClass() {
        return KineticBridgeBlockEntity.class;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }
}

