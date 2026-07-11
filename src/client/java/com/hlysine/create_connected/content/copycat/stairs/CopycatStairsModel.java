package com.hlysine.create_connected.content.copycat.stairs;

import com.hlysine.create_connected.content.copycat.ISimpleCopycatModel;
import com.zurrtum.create.client.infrastructure.model.CopycatModel;
import com.zurrtum.create.content.decoration.copycat.CopycatBlock;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;

import java.util.List;

import static com.hlysine.create_connected.content.copycat.ISimpleCopycatModel.MutableCullFace.*;

// Rewritten for MC 1.21.11's new "unbaked model parts" pipeline - see CopycatBlockModel's comment
// and PORTING_NOTES.md "CopycatModel architectural rewrite" for the general shape of this change.
public class CopycatStairsModel extends CopycatModel implements ISimpleCopycatModel {

    public CopycatStairsModel(BlockState state, BlockStateModel.UnbakedRoot unbaked) {
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
        int facing = (int) state.getValue(StairBlock.FACING).toYRot();
        boolean top = state.getValue(StairBlock.HALF) == Half.TOP;
        StairsShape shape = state.getValue(StairBlock.SHAPE);

        switch (shape) {
            case STRAIGHT -> {
                assemblePiece(part, builder, facing, top,
                        vec3(0, 0, 0),
                        aabb(16, 4, 8),
                        cull(UP | SOUTH)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(0, 4, 0),
                        aabb(16, 4, 8).move(0, 12, 0),
                        cull(DOWN | SOUTH)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(0, 0, 8),
                        aabb(16, 8, 8).move(0, 0, 8),
                        cull(UP | NORTH)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(0, 8, 8),
                        aabb(16, 8, 4).move(0, 8, 0),
                        cull(DOWN | SOUTH)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(0, 8, 12),
                        aabb(16, 8, 4).move(0, 8, 12),
                        cull(DOWN | NORTH)
                );
            }
            case INNER_LEFT -> {
                assemblePiece(part, builder, facing, top,
                        vec3(0, 0, 0),
                        aabb(8, 4, 8),
                        cull(UP | SOUTH | EAST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(0, 4, 0),
                        aabb(8, 4, 8).move(0, 12, 0),
                        cull(DOWN | SOUTH | EAST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(0, 0, 8),
                        aabb(16, 8, 8).move(0, 0, 8),
                        cull(UP | NORTH)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(8, 8, 8),
                        aabb(8, 8, 8).move(8, 8, 8),
                        cull(DOWN | NORTH | WEST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(0, 8, 12),
                        aabb(8, 8, 4).move(0, 8, 12),
                        cull(DOWN | NORTH | EAST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(0, 8, 8),
                        aabb(8, 8, 4).move(0, 8, 0),
                        cull(DOWN | SOUTH | EAST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(12, 8, 0),
                        aabb(4, 8, 8).move(12, 8, 0),
                        cull(DOWN | SOUTH | WEST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(8, 8, 0),
                        aabb(4, 8, 8).move(0, 8, 0),
                        cull(DOWN | SOUTH | EAST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(8, 0, 0),
                        aabb(8, 8, 8).move(8, 0, 0),
                        cull(UP | SOUTH | WEST)
                );
            }
            case INNER_RIGHT -> {
                assemblePiece(part, builder, facing, top,
                        vec3(8, 0, 0),
                        aabb(8, 4, 8).move(8, 0, 0),
                        cull(UP | SOUTH | WEST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(8, 4, 0),
                        aabb(8, 4, 8).move(8, 12, 0),
                        cull(DOWN | SOUTH | WEST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(0, 0, 8),
                        aabb(16, 8, 8).move(0, 0, 8),
                        cull(UP | NORTH)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(0, 8, 8),
                        aabb(8, 8, 8).move(0, 8, 8),
                        cull(DOWN | NORTH | EAST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(8, 8, 12),
                        aabb(8, 8, 4).move(8, 8, 12),
                        cull(DOWN | NORTH | WEST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(8, 8, 8),
                        aabb(8, 8, 4).move(8, 8, 0),
                        cull(DOWN | SOUTH | WEST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(4, 8, 0),
                        aabb(4, 8, 8).move(12, 8, 0),
                        cull(DOWN | SOUTH | WEST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(0, 8, 0),
                        aabb(4, 8, 8).move(0, 8, 0),
                        cull(DOWN | SOUTH | EAST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(0, 0, 0),
                        aabb(8, 8, 8).move(0, 0, 0),
                        cull(UP | SOUTH | EAST)
                );
            }
            case OUTER_LEFT -> {
                assemblePiece(part, builder, facing, top,
                        vec3(0, 0, 0),
                        aabb(8, 4, 16).move(0, 0, 0),
                        cull(UP | EAST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(0, 4, 0),
                        aabb(8, 4, 16).move(0, 12, 0),
                        cull(DOWN | EAST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(8, 0, 0),
                        aabb(8, 4, 8).move(8, 0, 0),
                        cull(UP | SOUTH | WEST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(8, 4, 0),
                        aabb(8, 4, 8).move(8, 12, 0),
                        cull(DOWN | SOUTH | WEST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(8, 0, 8),
                        aabb(8, 8, 8).move(8, 0, 8),
                        cull(UP | NORTH | WEST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(12, 8, 12),
                        aabb(4, 8, 4).move(12, 8, 12),
                        cull(DOWN | NORTH | WEST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(8, 8, 12),
                        aabb(4, 8, 4).move(0, 8, 12),
                        cull(DOWN | NORTH | EAST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(12, 8, 8),
                        aabb(4, 8, 4).move(12, 8, 0),
                        cull(DOWN | SOUTH | WEST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(8, 8, 8),
                        aabb(4, 8, 4).move(0, 8, 0),
                        cull(DOWN | SOUTH | EAST)
                );
            }
            case OUTER_RIGHT -> {
                assemblePiece(part, builder, facing, top,
                        vec3(8, 0, 0),
                        aabb(8, 4, 16).move(8, 0, 0),
                        cull(UP | WEST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(8, 4, 0),
                        aabb(8, 4, 16).move(8, 12, 0),
                        cull(DOWN | WEST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(0, 0, 0),
                        aabb(8, 4, 8).move(0, 0, 0),
                        cull(UP | EAST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(0, 4, 0),
                        aabb(8, 4, 8).move(0, 12, 0),
                        cull(DOWN | EAST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(0, 0, 8),
                        aabb(8, 8, 8).move(0, 0, 8),
                        cull(UP | NORTH | EAST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(4, 8, 12),
                        aabb(4, 8, 4).move(12, 8, 12),
                        cull(DOWN | NORTH | WEST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(0, 8, 12),
                        aabb(4, 8, 4).move(0, 8, 12),
                        cull(DOWN | NORTH | EAST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(4, 8, 8),
                        aabb(4, 8, 4).move(12, 8, 0),
                        cull(DOWN | SOUTH | WEST)
                );
                assemblePiece(part, builder, facing, top,
                        vec3(0, 8, 8),
                        aabb(4, 8, 4).move(0, 8, 0),
                        cull(DOWN | SOUTH | EAST)
                );
            }
        }
    }

}
