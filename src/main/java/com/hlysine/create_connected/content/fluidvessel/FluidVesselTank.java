package com.hlysine.create_connected.content.fluidvessel;

import com.zurrtum.create.foundation.fluid.FluidTank;
import com.zurrtum.create.infrastructure.fluids.FluidStack;

import java.util.function.Consumer;

// Create Fly dropped the old NeoForge-oriented "SmartFluidTank(capacity, onChange)" helper
// entirely - com.zurrtum.create.foundation.fluid.FluidTank (its real replacement, own
// infrastructure.fluids.FluidStack based) has no update-callback constructor at all anymore.
// Real Create Fly reimplements the "notify on change" behaviour per use-case by overriding
// markDirty() (see CreativeFluidTankBlockEntity.CreativeFluidTankInventory for the reference this
// was copied from), so this does the same for our non-creative vessel tank instead of inventing a
// different shape.
public class FluidVesselTank extends FluidTank {
    private final Consumer<FluidStack> updateCallback;

    public FluidVesselTank(int capacity, Consumer<FluidStack> updateCallback) {
        super(capacity);
        this.updateCallback = updateCallback;
    }

    @Override
    public void markDirty() {
        updateCallback.accept(getFluid());
    }
}
