package com.zurrtum.create.client.content.redstone.link;

import com.hlysine.create_connected.registries.CCBlocks;
import com.zurrtum.create.catnip.data.Couple;
import com.zurrtum.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class ConnectedAnalogLeverLinkBehaviour extends ConnectedLinkBehaviour {
    private static final Couple<RedstoneLinkNetworkHandler.Frequency> EMPTY_NETWORK =
            Couple.create(RedstoneLinkNetworkHandler.Frequency.EMPTY, RedstoneLinkNetworkHandler.Frequency.EMPTY);

    public ConnectedAnalogLeverLinkBehaviour(SmartBlockEntity be) {
        super(be);
    }

    private boolean isLinkedAnalogLever() {
        return blockEntity.getBlockState().is(CCBlocks.LINKED_ANALOG_LEVER);
    }

    @Override
    public boolean testHit(Boolean first, Vec3 hit) {
        return isLinkedAnalogLever() && behaviour != null && super.testHit(first, hit);
    }

    @Override
    public Couple<RedstoneLinkNetworkHandler.Frequency> getNetworkKey() {
        return behaviour == null ? EMPTY_NETWORK : super.getNetworkKey();
    }

    @Override
    public ItemStack getFirstStack() {
        return behaviour == null ? ItemStack.EMPTY : super.getFirstStack();
    }

    @Override
    public ItemStack getLastStack() {
        return behaviour == null ? ItemStack.EMPTY : super.getLastStack();
    }

    @Override
    public void setFrequency(boolean first, ItemStack heldItem) {
        if (behaviour != null && isLinkedAnalogLever()) {
            super.setFrequency(first, heldItem);
        }
    }
}
