package com.hlysine.create_connected.content.inventorybridge;

import com.zurrtum.create.client.foundation.blockEntity.behaviour.filtering.SidedFilteringBehaviour;

public class InventoryBridgeFilteringBehaviour extends SidedFilteringBehaviour {

    public InventoryBridgeFilteringBehaviour(InventoryBridgeBlockEntity be) {
        super(be, new InventoryBridgeFilterSlot());
    }
}
