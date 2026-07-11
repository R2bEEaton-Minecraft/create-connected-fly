package com.hlysine.create_connected.content.fluidvessel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CreativeFluidVesselBlockEntity extends FluidVesselBlockEntity {

    public CreativeFluidVesselBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected CreativeFluidVesselTank createInventory() {
        return new CreativeFluidVesselTank(getCapacityMultiplier(), this::onFluidStackChanged);
    }

    // addToGoggleTooltip's old override (always false) moved off this class entirely - see
    // FluidVesselBlockEntity's class comment. Since CREATIVE_FLUID_VESSEL simply has no tooltip
    // behaviour registered in CreateConnectedClient (unlike FLUID_VESSEL), the same "no goggle info"
    // outcome falls out naturally: BlockEntityBehaviour.get(..., TooltipBehaviour.TYPE) just returns
    // null for it, so the client-side instanceof IHaveGoggleInformation check is never true.

}
