package com.hlysine.create_connected.mixin;

import com.hlysine.create_connected.registries.PreciseItemUseOverrides;
import com.zurrtum.create.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// com.zurrtum.create.foundation.block.ItemUseOverrides (the original mixin target) does not
// exist in Create Fly - it moved this "precise hit location" pattern into direct mixins on
// vanilla's ServerPlayerGameMode (see com.zurrtum.create.mixin.ServerPlayerGameModeMixin in the
// real jar, which mixes into the same useItemOn method for its own similar overrides). Re-target
// there: swap the incoming BlockHitResult for a re-picked, more precise one before block-use
// dispatch runs, for any block registered in PreciseItemUseOverrides.OVERRIDES.
@Mixin(ServerPlayerGameMode.class)
public class ItemUseOverridesMixin {
    @ModifyVariable(
            method = "useItemOn",
            at = @At("HEAD"),
            argsOnly = true
    )
    private BlockHitResult preciseHitLocation(BlockHitResult hitResult, ServerPlayer player, Level level, ItemStack stack, InteractionHand hand) {
        BlockState state = level.getBlockState(hitResult.getBlockPos());
        Identifier id = RegisteredObjectsHelper.getKeyOrThrow(state.getBlock());
        if (PreciseItemUseOverrides.OVERRIDES.contains(id)) {
            HitResult preciseHitResult = player.pick(player.blockInteractionRange(), 1, false);
            if (preciseHitResult instanceof BlockHitResult preciseBlockHitResult
                    && preciseBlockHitResult.getBlockPos().equals(hitResult.getBlockPos())) {
                // Ensures that preciseBlockHitResult has the same block pos as hitResult to
                // prevent an unexpected block from being selected because of de-synced rotation
                return preciseBlockHitResult;
            }
        }
        return hitResult;
    }
}
