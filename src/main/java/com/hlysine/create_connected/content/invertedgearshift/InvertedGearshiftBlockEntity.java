package com.hlysine.create_connected.content.invertedgearshift;

import com.zurrtum.create.content.kinetics.transmission.GearshiftBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class InvertedGearshiftBlockEntity extends GearshiftBlockEntity {

    // Real GearshiftBlockEntity's constructor is 2-arg (BlockPos, BlockState) - see
    // LinkedAnalogLeverBlockEntity.java for the full writeup on this pattern.
    public InvertedGearshiftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (hasSource()) {
            if (face != getSourceFacing() && !getBlockState().getValue(BlockStateProperties.POWERED))
                return -1;
        }
        return 1;
    }

}
