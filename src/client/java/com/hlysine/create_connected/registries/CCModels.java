package com.hlysine.create_connected.registries;

import com.hlysine.create_connected.content.copycat.beam.CopycatBeamModel;
import com.hlysine.create_connected.content.copycat.block.CopycatBlockModel;
import com.hlysine.create_connected.content.copycat.board.CopycatBoardModel;
import com.hlysine.create_connected.content.copycat.fence.CopycatFenceModel;
import com.hlysine.create_connected.content.copycat.fencegate.CopycatFenceGateModel;
import com.hlysine.create_connected.content.copycat.slab.CopycatSlabModel;
import com.hlysine.create_connected.content.copycat.stairs.CopycatStairsModel;
import com.hlysine.create_connected.content.copycat.verticalstep.CopycatVerticalStepModel;
import com.hlysine.create_connected.content.copycat.wall.CopycatWallModel;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

// Client-only model-wrapping registry, mirroring the real Create Fly AllModels.ALL pattern exactly
// (a Map<Block, BiFunction<BlockState, UnbakedRoot, UnbakedRoot>> consulted by a mixin into
// vanilla's BlockStateModelLoader - see BlockStateModelLoaderMixin and PORTING_NOTES.md
// "CopycatModel architectural rewrite" for why this wiring is needed at all: none of our 9
// CopycatXxxModel classes were ever actually instantiated anywhere before this, since Create Fly's
// own equivalent wiring (AllModels.ALL + its BlockStateModelLoaderMixin) is entirely Create Fly's
// own and doesn't automatically pick up this mod's blocks).
public class CCModels {
    public static final Map<Block, BiFunction<BlockState, BlockStateModel.UnbakedRoot, BlockStateModel.UnbakedRoot>> ALL = new HashMap<>();

    private static void register(Block block, BiFunction<BlockState, BlockStateModel.UnbakedRoot, BlockStateModel.UnbakedRoot> resolver) {
        ALL.put(block, resolver);
    }

    public static void register() {
        register(CCBlocks.COPYCAT_BLOCK, CopycatBlockModel::new);
        register(CCBlocks.COPYCAT_SLAB, CopycatSlabModel::new);
        register(CCBlocks.COPYCAT_BEAM, CopycatBeamModel::new);
        register(CCBlocks.COPYCAT_VERTICAL_STEP, CopycatVerticalStepModel::new);
        register(CCBlocks.COPYCAT_STAIRS, CopycatStairsModel::new);
        register(CCBlocks.COPYCAT_FENCE, CopycatFenceModel::new);
        register(CCBlocks.COPYCAT_WALL, CopycatWallModel::new);
        register(CCBlocks.COPYCAT_FENCE_GATE, CopycatFenceGateModel::new);
        register(CCBlocks.COPYCAT_BOARD, CopycatBoardModel::new);
    }
}
