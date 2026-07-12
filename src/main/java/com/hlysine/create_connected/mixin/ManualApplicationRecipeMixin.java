package com.hlysine.create_connected.mixin;

import com.hlysine.create_connected.config.CServer;
import com.zurrtum.create.content.kinetics.deployer.ManualApplicationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Original mixin target com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe no
// longer exists as a mixin-able injection point in this shape - the "consume held item, give back
// its crafting remainder" logic now lives directly in ManualApplicationHelper's own
// manualApplicationRecipesApplyInWorld method (verified against the real jar), so retarget there
// with its real (non-NeoForge-event) parameters instead of PlayerInteractEvent.RightClickBlock.
@Mixin(ManualApplicationHelper.class)
public class ManualApplicationRecipeMixin {
    @Inject(
            method = "manualApplicationRecipesApplyInWorld",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V")
    )
    private static void craftingRemainingItemOnApplication(Level level, Player player, ItemStack heldItem, InteractionHand hand,
                                                             BlockHitResult hit, BlockPos pos, CallbackInfoReturnable<InteractionResult> cir) {
        if (!CServer.ApplicationRemainingItemFix.get()) return;

        // ItemStack.hasCraftingRemainingItem()/getCraftingRemainingItem() are gone - the concept moved
        // to a plain Item-level accessor, Item.getCraftingRemainder() (confirmed via javap), which
        // already returns ItemStack.EMPTY when there is none, so no separate "has" check is needed.
        ItemStack leftover = heldItem.getItem().getCraftingRemainder();

        heldItem.shrink(1);

        if (heldItem.isEmpty()) {
            player.setItemInHand(hand, leftover);
        } else {
            heldItem.grow(1); // Create shrinks the stack again after this inject
            if (!player.getInventory().add(leftover)) {
                player.drop(leftover, false);
            }
        }
    }
}
