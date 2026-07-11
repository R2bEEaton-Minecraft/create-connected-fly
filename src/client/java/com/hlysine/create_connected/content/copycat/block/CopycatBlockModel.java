package com.hlysine.create_connected.content.copycat.block;

import com.zurrtum.create.client.infrastructure.model.CopycatModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.List;

public class CopycatBlockModel extends CopycatModel {

    public CopycatBlockModel(BakedModel originalModel) {
        super(originalModel);
    }

    @Override
    protected List<BakedQuad> getCroppedQuads(BlockState state, Direction side, RandomSource rand, BlockState material,
                                              ModelData wrappedData, RenderType renderType) {
        BakedModel model = getModelOf(material);
        List<BakedQuad> templateQuads = model.getQuads(material, side, rand, wrappedData, renderType);

        // BakedQuad is an immutable record in this MC version (verified via the real Create Fly
        // sources - see BakedModelHelper.cropAndMove's record-accessor usage), so the defensive
        // clone() the old NeoForge BakedQuadHelper did (a mutable int[] vertex array under the
        // hood back then) has no equivalent need anymore - reusing the same quad instances is safe.
        return new ArrayList<>(templateQuads);
    }
}
