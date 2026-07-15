package com.hlysine.create_connected.content.linkedtransmitter;

import com.hlysine.create_connected.mixin.linkedtransmitter.AnalogLeverBlockEntityAccessor;
import com.hlysine.create_connected.mixin.linkedtransmitter.BlockEntityTypeAccessor;
import com.zurrtum.create.content.redstone.analogLever.AnalogLeverBlockEntity;
import com.zurrtum.create.content.redstone.link.ServerLinkBehaviour;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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

    // Real AnalogLeverBlockEntity's constructor is 2-arg (BlockPos, BlockState) and hardcodes
    // Create's ANALOG_LEVER block-entity type. Leaving that type in place broke everything keyed
    // by the type: the entity saved to disk as "create:analog_lever" (so relogging recreated it as
    // a plain AnalogLeverBlockEntity, losing link data and interactions), Create Fly's
    // AnalogLeverVisual suppressed the vanilla render pass that draws the frequency items, and
    // CCBlockEntityTypes.LINKED_ANALOG_LEVER renderer/behaviour registrations never applied.
    // Retag through the BlockEntityTypeAccessor mixin since no vanilla API can change the type
    // after construction.
    public LinkedAnalogLeverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(pos, state);
        ((BlockEntityTypeAccessor) this).create_connected$setType(type);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
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

    public void copyLinkDataFrom(LinkedAnalogLeverBlockEntity other) {
        containsBase = other.containsBase;
        if (link != null && other.link != null) {
            link.setFrequency(true, other.link.frequencyFirst.getStack());
            link.setFrequency(false, other.link.frequencyLast.getStack());
        }
    }

    public void transmit() {
        if (link != null)
            link.notifySignalChange();
    }

    @Override
    public void tick() {
        int previousLastChange = ((AnalogLeverBlockEntityAccessor) this).create_connected$getLastChange();
        super.tick();
        if (previousLastChange > 0 && ((AnalogLeverBlockEntityAccessor) this).create_connected$getLastChange() == 0 && !level.isClientSide()) {
            transmit();
            boolean powered = getState() > 0;
            if (getBlockState().getValue(BlockStateProperties.POWERED) != powered) {
                level.setBlock(worldPosition, getBlockState().setValue(BlockStateProperties.POWERED, powered), Block.UPDATE_ALL);
                if (level.getBlockEntity(worldPosition) instanceof LinkedAnalogLeverBlockEntity replacement && replacement != this) {
                    replacement.copyLinkDataFrom(this);
                    replacement.transmit();
                    replacement.sendData();
                }
            }
        }
    }

    @Override
    public void write(ValueOutput tag, boolean clientPacket) {
        tag.putBoolean("ContainsBase", containsBase);
        super.write(tag, clientPacket);
    }

    @Override
    protected void read(ValueInput tag, boolean clientPacket) {
        containsBase = tag.getBooleanOr("ContainsBase", true);
        super.read(tag, clientPacket);
    }
}
