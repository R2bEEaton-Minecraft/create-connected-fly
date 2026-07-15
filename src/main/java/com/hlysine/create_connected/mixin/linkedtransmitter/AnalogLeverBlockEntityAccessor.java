package com.hlysine.create_connected.mixin.linkedtransmitter;

import com.zurrtum.create.content.redstone.analogLever.AnalogLeverBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AnalogLeverBlockEntity.class)
public interface AnalogLeverBlockEntityAccessor {
    @Accessor("lastChange")
    int create_connected$getLastChange();
}
