package com.hlysine.create_connected.content.copycat.block;

import com.zurrtum.create.client.infrastructure.model.CopycatModel;
import com.zurrtum.create.content.decoration.copycat.CopycatBlock;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

// Rewritten for MC 1.21.11's new "unbaked model parts" pipeline - CopycatModel's real API changed
// entirely (was a BakedModel-wrapping getQuads/getCroppedQuads(state, side, rand, material,
// ModelData, RenderType) NeoForge-era shape, is now addPartsWithInfo(..., List<BlockModelPart>)
// building parts via BlockStateModel.collectParts/BlockModelPart.getQuads(Direction) - see
// PORTING_NOTES.md "CopycatModel architectural rewrite"). This variant has no shape cropping at
// all (a full-cube copycat just mirrors whatever the material's own model parts already are), so
// it delegates straight to the base class's addModelParts() helper - same as how the real
// CopycatPanelModel handles its "trapdoor material" special case.
public class CopycatBlockModel extends CopycatModel {

    public CopycatBlockModel(BlockState state, BlockStateModel.UnbakedRoot unbaked) {
        super(state, unbaked);
    }

    @Override
    protected void addPartsWithInfo(
            BlockAndTintGetter world,
            BlockPos pos,
            BlockState state,
            CopycatBlock block,
            BlockState material,
            RandomSource random,
            List<BlockModelPart> parts
    ) {
        addModelParts(world, pos, material, random, getModelOf(material), parts);
    }
}
