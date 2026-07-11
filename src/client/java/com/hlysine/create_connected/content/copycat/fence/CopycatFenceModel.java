package com.hlysine.create_connected.content.copycat.fence;

import com.hlysine.create_connected.content.copycat.ISimpleCopycatModel;
import com.zurrtum.create.client.infrastructure.model.CopycatModel;
import com.zurrtum.create.catnip.data.Iterate;
import com.zurrtum.create.content.decoration.copycat.CopycatBlock;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static com.hlysine.create_connected.content.copycat.ISimpleCopycatModel.MutableCullFace.*;
import static com.hlysine.create_connected.content.copycat.fence.CopycatFenceBlock.byDirection;

// Rewritten for MC 1.21.11's new "unbaked model parts" pipeline - see CopycatBlockModel's comment
// and PORTING_NOTES.md "CopycatModel architectural rewrite" for the general shape of this change.
public class CopycatFenceModel extends CopycatModel implements ISimpleCopycatModel {

    public CopycatFenceModel(BlockState state, BlockStateModel.UnbakedRoot unbaked) {
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
        for (BlockModelPart part : getMaterialParts(world, pos, material, random, getModelOf(material))) {
            QuadCollection.Builder builder = new QuadCollection.Builder();
            assembleParts(state, part, builder);
            parts.add(new SimpleModelWrapper(builder.build(), part.useAmbientOcclusion(), part.particleIcon()));
        }
    }

    private void assembleParts(BlockState state, BlockModelPart part, QuadCollection.Builder builder) {
        for (Direction direction : Iterate.horizontalDirections) {
            assemblePiece(part, builder, (int) direction.toYRot(), false,
                    vec3(6, 0, 6),
                    aabb(2, 16, 2),
                    cull(SOUTH | EAST)
            );
        }

        for (Direction direction : Iterate.horizontalDirections) {
            if (!state.getValue(byDirection(direction))) continue;

            int rot = (int) direction.toYRot();
            assemblePiece(part, builder, rot, false,
                    vec3(7, 6, 10),
                    aabb(1, 1, 6),
                    cull(UP | NORTH | EAST)
            );
            assemblePiece(part, builder, rot, false,
                    vec3(8, 6, 10),
                    aabb(1, 1, 6).move(15, 0, 0),
                    cull(UP | NORTH | WEST)
            );
            assemblePiece(part, builder, rot, false,
                    vec3(7, 7, 10),
                    aabb(1, 2, 6).move(0, 14, 0),
                    cull(DOWN | NORTH | EAST)
            );
            assemblePiece(part, builder, rot, false,
                    vec3(8, 7, 10),
                    aabb(1, 2, 6).move(15, 14, 0),
                    cull(DOWN | NORTH | WEST)
            );

            assemblePiece(part, builder, rot, false,
                    vec3(7, 12, 10),
                    aabb(1, 1, 6),
                    cull(UP | NORTH | EAST)
            );
            assemblePiece(part, builder, rot, false,
                    vec3(8, 12, 10),
                    aabb(1, 1, 6).move(15, 0, 0),
                    cull(UP | NORTH | WEST)
            );
            assemblePiece(part, builder, rot, false,
                    vec3(7, 13, 10),
                    aabb(1, 2, 6).move(0, 14, 0),
                    cull(DOWN | NORTH | EAST)
            );
            assemblePiece(part, builder, rot, false,
                    vec3(8, 13, 10),
                    aabb(1, 2, 6).move(15, 14, 0),
                    cull(DOWN | NORTH | WEST)
            );
        }
    }

}
