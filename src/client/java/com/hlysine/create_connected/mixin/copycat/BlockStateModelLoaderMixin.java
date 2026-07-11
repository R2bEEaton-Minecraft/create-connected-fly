package com.hlysine.create_connected.mixin.copycat;

import com.hlysine.create_connected.registries.CCModels;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

// Client-only mixin mirroring Create Fly's own real com.zurrtum.create.client.mixin.
// BlockStateModelLoaderMixin exactly (verified in the real sources jar), just consulting this
// mod's own CCModels.ALL registry instead of Create Fly's AllModels.ALL - see PORTING_NOTES.md
// "CopycatModel architectural rewrite" for why this is needed: without it, none of our
// CopycatXxxModel classes are ever actually used, since Create Fly's own equivalent wiring only
// wraps ITS OWN copycat-family blocks (registered in its own AllModels.ALL), not this mod's.
@Mixin(BlockStateModelLoader.class)
public class BlockStateModelLoaderMixin {
    @Inject(method = "loadBlockStateDefinitionStack(Lnet/minecraft/resources/Identifier;Lnet/minecraft/world/level/block/state/StateDefinition;Ljava/util/List;)Lnet/minecraft/client/resources/model/BlockStateModelLoader$LoadedModels;", at = @At(value = "NEW", target = "(Ljava/util/Map;)Lnet/minecraft/client/resources/model/BlockStateModelLoader$LoadedModels;"))
    private static void create_connected$replace(
            Identifier identifier,
            StateDefinition<Block, BlockState> stateDefinition,
            List<?> list,
            CallbackInfoReturnable<BlockStateModelLoader.LoadedModels> cir,
            @Local Map<BlockState, BlockStateModel.UnbakedRoot> models
    ) {
        BiFunction<BlockState, BlockStateModel.UnbakedRoot, BlockStateModel.UnbakedRoot> factory = CCModels.ALL.get(stateDefinition.getOwner());
        if (factory != null) {
            models.replaceAll(factory);
        }
    }
}
