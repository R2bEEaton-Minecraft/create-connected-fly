package com.hlysine.create_connected.mixin.linkedtransmitter;

import com.hlysine.create_connected.registries.CCBlocks;
import com.zurrtum.create.AllItems;
import com.zurrtum.create.client.content.redstone.link.LinkBehaviour;
import com.zurrtum.create.client.content.redstone.link.LinkHandler;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.catnip.data.Iterate;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LinkHandler.class)
public class LinkHandlerMixin {
    @Inject(method = "onBlockActivated", at = @At("HEAD"), cancellable = true)
    private static void create_connected$letAnalogLeverClicksPass(Level world,
                                                                  LocalPlayer player,
                                                                  InteractionHand hand,
                                                                  BlockHitResult ray,
                                                                  CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.is(AllItems.WRENCH))
            return;

        BlockPos pos = ray.getBlockPos();
        if (!world.getBlockState(pos).is(CCBlocks.LINKED_ANALOG_LEVER))
            return;

        LinkBehaviour behaviour = BlockEntityBehaviour.get(world, pos, LinkBehaviour.TYPE);
        if (behaviour == null) {
            cir.setReturnValue(null);
            cir.cancel();
            return;
        }

        boolean targetingSlot = false;
        for (boolean first : Iterate.trueAndFalse) {
            if (behaviour.testHit(first, ray.getLocation())) {
                targetingSlot = true;
                break;
            }
        }

        // Let normal analog-lever interaction own clicks that miss the frequency slots.
        if (!targetingSlot) {
            cir.setReturnValue(null);
            cir.cancel();
        }
    }
}
