package com.hlysine.create_connected.content.overstressclutch;

import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.TooltipBehaviour;
import net.minecraft.network.chat.Component;

import java.util.List;

import static com.hlysine.create_connected.content.overstressclutch.OverstressClutchBlock.STATE;
import static com.hlysine.create_connected.content.overstressclutch.OverstressClutchBlock.ClutchState;

// addToGoggleTooltip used to live directly on OverstressClutchBlockEntity as an "addToTooltip"
// override, but that never actually overrode anything real either (IHaveGoggleInformation is a
// client-only interface with the method named addToGoggleTooltip, not addToTooltip - confirmed by
// reading the real interface) - the same cross-boundary bug already fixed for
// FluidVesselBlockEntity/KineticBatteryBlockEntity. Extracted here, mirroring those fixes: registered
// per-block-entity-type via BlockEntityBehaviour.addClient() in
// CreateConnectedClient.onInitializeClient(), not implemented on the block entity itself. The actual
// tooltip content (uncoupledTooltipHook) was already correctly split out into
// OverstressClutchBlockEntityClient in an earlier session - this class just supplies the real
// addToGoggleTooltip entry point that calls it.
public class OverstressClutchTooltipBehaviour extends TooltipBehaviour<OverstressClutchBlockEntity> implements IHaveGoggleInformation {
    public OverstressClutchTooltipBehaviour(OverstressClutchBlockEntity be) {
        super(be);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (blockEntity.getBlockState().getValue(STATE) == ClutchState.UNCOUPLED) {
            OverstressClutchBlockEntity.uncoupledTooltipHook.accept(blockEntity, tooltip);
            return true;
        }
        return false;
    }
}
