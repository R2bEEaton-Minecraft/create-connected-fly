package com.hlysine.create_connected.content.linkedtransmitter;

import com.hlysine.create_connected.mixin.linkedtransmitter.AnalogLeverBlockEntityAccessor;
import com.zurrtum.create.content.redstone.analogLever.AnalogLeverBlockEntity;
import com.zurrtum.create.content.redstone.link.ServerLinkBehaviour;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;

public class LinkedAnalogLeverBlockEntity extends AnalogLeverBlockEntity {
    /**
     * set to false if the module item is already returned to player via wrenching
     */
    public boolean containsBase = true;
    private ServerLinkBehaviour link;

    public LinkedAnalogLeverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        createLink();
        behaviours.add(link);
    }

    @Override
    public void initialize() {
        super.initialize();
        transmit();
    }

    protected void createLink() {
        link = ServerLinkBehaviour.transmitter(this, this::getState);
    }

    public void transmit() {
        if (link != null)
            link.notifySignalChange();
    }

    private int lastChange() {
        return ((AnalogLeverBlockEntityAccessor) this).getLastChange();
    }

    @Override
    public void tick() {
        int prevTick = lastChange();
        super.tick();
        if (prevTick > 0 && lastChange() == 0) {
            if (!level.isClientSide()) {
                transmit();
                level.setBlock(worldPosition, getBlockState().setValue(BlockStateProperties.POWERED, getState() > 0), Block.UPDATE_ALL);
            }
        }
    }
}
