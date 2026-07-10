package com.hlysine.create_connected.mixin.linkedtransmitter;

import com.zurrtum.create.content.redstone.analogLever.AnalogLeverBlockEntity;
import com.zurrtum.create.catnip.animation.LerpedFloat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AnalogLeverBlockEntity.class, remap = false)
public interface AnalogLeverBlockEntityAccessor {
    @Accessor
    int getLastChange();

    @Accessor
    LerpedFloat getClientState();
}
