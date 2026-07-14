package com.zurrtum.create.client.content.redstone.link;

import com.hlysine.create_connected.content.linkedtransmitter.LinkedTransmitterFrequencySlot;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;

public class ConnectedLinkBehaviour extends LinkBehaviour {
    public ConnectedLinkBehaviour(SmartBlockEntity be) {
        super(be);
        firstSlot = new LinkedTransmitterFrequencySlot(true);
        secondSlot = new LinkedTransmitterFrequencySlot(false);
    }
}
