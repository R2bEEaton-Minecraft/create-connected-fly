package com.hlysine.create_connected.mixin.linkedtransmitter;

import com.hlysine.create_connected.content.linkedtransmitter.LinkedAnalogLeverBlockEntity;
import com.zurrtum.create.client.content.redstone.link.ConnectedAnalogLeverLinkBehaviour;
import com.zurrtum.create.client.content.redstone.link.LinkBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LinkedAnalogLeverBlockEntity.class)
public class LinkedAnalogLeverBlockEntityMixin {
    @Inject(method = "initialize", at = @At("TAIL"))
    private void create_connected$attachClientLinkBehaviour(CallbackInfo ci) {
        LinkedAnalogLeverBlockEntity be = (LinkedAnalogLeverBlockEntity) (Object) this;
        if (!be.hasLevel() || !be.getLevel().isClientSide())
            return;
        if (be.getBehaviour(LinkBehaviour.TYPE) != null)
            return;
        be.attachBehaviourLate(new ConnectedAnalogLeverLinkBehaviour(be));
    }
}
