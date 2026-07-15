package com.hlysine.create_connected.mixin.fluidvessel;

import com.hlysine.create_connected.content.fluidvessel.FluidVesselBlock;
import com.zurrtum.create.content.kinetics.steamEngine.SteamEngineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SteamEngineBlock.class, remap = false)
public class SteamEngineBlockMixin {
    @Inject(
            at = @At("TAIL"),
            method = "onPlace"
    )
    private void onPlaceVessel(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving, CallbackInfo ci) {
        FluidVesselBlock.updateBoilerState(pState, pLevel, pPos.relative(SteamEngineBlock.getFacing(pState).getOpposite()));
    }

    @Inject(
            at = @At("TAIL"),
            method = "affectNeighborsAfterRemoval"
    )
    private void onRemoveVessel(BlockState pState, ServerLevel pLevel, BlockPos pPos, boolean pIsMoving, CallbackInfo ci) {
        FluidVesselBlock.updateBoilerState(pState, pLevel, pPos.relative(SteamEngineBlock.getFacing(pState).getOpposite()));
    }

    @Inject(
            at = @At("HEAD"),
            method = "canAttach",
            cancellable = true,
            remap = true
    )
    private static void canAttach(LevelReader pReader, BlockPos pPos, Direction pDirection, CallbackInfoReturnable<Boolean> cir) {
        BlockPos blockpos = pPos.relative(pDirection);
        if (pReader.getBlockState(blockpos).getBlock() instanceof FluidVesselBlock) {
            cir.setReturnValue(true);
        }
    }
}
