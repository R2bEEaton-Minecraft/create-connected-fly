package com.hlysine.create_connected.content.fluidvessel;

import com.zurrtum.create.foundation.fluid.FluidTank;
import com.zurrtum.create.infrastructure.fluids.FluidStack;

import java.util.function.Consumer;

// Real Create Fly's equivalent "creative infinite tank" is
// CreativeFluidTankBlockEntity.CreativeFluidTankInventory (a private nested class, not reusable
// from here) - this mirrors that same shape (infinite insert/extract, always full on markDirty)
// for our own FluidVesselTank base instead of depending on Create Fly's non-exported nested class.
public class CreativeFluidVesselTank extends FluidVesselTank {
    public CreativeFluidVesselTank(int capacity, Consumer<FluidStack> updateCallback) {
        super(capacity, updateCallback);
    }

    @Override
    public void markDirty() {
        getFluid().setAmount(getMaxAmountPerStack());
        super.markDirty();
    }

    @Override
    public int insert(FluidStack stack) {
        return stack.getAmount();
    }

    @Override
    public int insert(FluidStack stack, int maxAmount) {
        return maxAmount;
    }

    @Override
    public int countSpace(FluidStack stack) {
        return stack.getAmount();
    }

    @Override
    public int countSpace(FluidStack stack, int maxAmount) {
        return maxAmount;
    }

    @Override
    public int extract(FluidStack stack) {
        return stack.getAmount();
    }

    @Override
    public int extract(FluidStack stack, int maxAmount) {
        return maxAmount;
    }

    @Override
    public boolean preciseInsert(FluidStack stack) {
        return true;
    }

    @Override
    public boolean preciseExtract(FluidStack stack) {
        return true;
    }
}
