package com.hlysine.create_connected.content.fluidvessel;

import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.TooltipBehaviour;
import net.minecraft.network.chat.Component;

import java.util.List;

// com.zurrtum.create.client.api.goggles.IHaveGoggleInformation is client-only - FluidVesselBlockEntity
// used to implement it directly, a real cross-boundary bug (main-sourceset referencing a client-only
// type), caught only by a real ./gradlew compileJava run (the combined-classpath javac-direct
// workaround can't see this class of bug - see PORTING_NOTES.md). Extracted here, mirroring Create
// Fly's own real FluidTankTooltipBehaviour/AllBlockEntityBehaviours pattern: goggle tooltip info is
// registered per-block-entity-type via BlockEntityBehaviour.CLIENT_REGISTRY (see
// CreateConnectedClient.onInitializeClient()), not implemented directly on the block entity class.
public class FluidVesselTooltipBehaviour extends TooltipBehaviour<FluidVesselBlockEntity> implements IHaveGoggleInformation {
    public FluidVesselTooltipBehaviour(FluidVesselBlockEntity be) {
        super(be);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        FluidVesselBlockEntity controllerBE = blockEntity.getControllerBE();
        if (controllerBE == null)
            return false;
        if (controllerBE.boiler.addToGoggleTooltip(tooltip, isPlayerSneaking, controllerBE.getTotalTankSize()))
            return true;
        return containedFluidTooltip(tooltip, isPlayerSneaking, controllerBE.fluidCapability);
    }
}
