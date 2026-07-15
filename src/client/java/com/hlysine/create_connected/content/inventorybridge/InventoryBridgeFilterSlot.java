package com.hlysine.create_connected.content.inventorybridge;

import com.zurrtum.create.catnip.math.VecHelper;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class InventoryBridgeFilterSlot extends CenteredSideValueBoxTransform {

    public InventoryBridgeFilterSlot() {
        super((state, direction) -> state.getValue(InventoryBridgeBlock.AXIS) == direction.getAxis());
    }

    @Override
    protected Vec3 getSouthLocation() {
        // Match the original mod's high-on-face placement, but nudge slightly outward so Fly's
        // value-box item render does not get clipped inside the bridge's full cube face.
        return VecHelper.voxelSpace(8, 15.5, 15.9);
    }
}
