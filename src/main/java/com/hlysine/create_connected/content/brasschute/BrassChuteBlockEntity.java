package com.hlysine.create_connected.content.brasschute;

import com.hlysine.create_connected.mixin.brasschute.ChuteBlockEntityAccessor;
import com.zurrtum.create.content.logistics.chute.ChuteBlockEntity;
import com.zurrtum.create.content.logistics.chute.ChuteItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BrassChuteBlockEntity extends ChuteBlockEntity {
    public BrassChuteBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected int getExtractionAmount() {
        return 64;
    }

    public ChuteItemHandler itemHandler() {
        return ((ChuteBlockEntityAccessor) this).getItemHandler();
    }

    // NeoForge's RegisterCapabilitiesEvent registration is gone - this type is registered onto
    // Fabric's ItemStorage.SIDED via CCTransfer.register() instead (ChuteItemHandler already
    // implements Create Fly's own ItemInventory/vanilla Container, so it plugs straight into
    // InventoryStorage.of() the same way Create Fly's own CHUTE registration does in AllTransfer).
}
