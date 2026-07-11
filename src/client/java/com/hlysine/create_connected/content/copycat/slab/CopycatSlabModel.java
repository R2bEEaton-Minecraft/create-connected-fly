package com.hlysine.create_connected.content.copycat.slab;

import com.zurrtum.create.client.infrastructure.model.CopycatModel;
import com.zurrtum.create.client.foundation.model.BakedModelHelper;
import com.zurrtum.create.catnip.data.Iterate;
import com.zurrtum.create.content.decoration.copycat.CopycatBlock;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Consumer;

// Rewritten for MC 1.21.11's new "unbaked model parts" pipeline (see PORTING_NOTES.md
// "CopycatModel architectural rewrite" and CopycatBlockModel's comment for the general shape).
// Each old getCroppedQuads(state, side, ...) call for a given "side" (null = unculled, or one of
// the 6 directions) is now one QuadCollection.Builder bucket, built by querying
// part.getQuads(direction) for that same direction and applying the exact same crop/skip logic as
// before per quad.
public class CopycatSlabModel extends CopycatModel {

    protected static final AABB CUBE_AABB = new AABB(BlockPos.ZERO);

    public CopycatSlabModel(BlockState state, BlockStateModel.UnbakedRoot unbaked) {
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
        Direction facing = state.getOptionalValue(CopycatSlabBlock.SLAB_TYPE).isPresent() ? CopycatSlabBlock.getApparentDirection(state) : Direction.UP;
        boolean isDouble = state.getOptionalValue(CopycatSlabBlock.SLAB_TYPE).orElse(SlabType.BOTTOM) == SlabType.DOUBLE;

        for (BlockModelPart part : getMaterialParts(world, pos, material, random, getModelOf(material))) {
            QuadCollection.Builder builder = new QuadCollection.Builder();

            for (boolean front : Iterate.trueAndFalse)
                assemblePiece(facing, part, builder, front, false, isDouble);
            if (isDouble)
                for (boolean front : Iterate.trueAndFalse)
                    assemblePiece(facing, part, builder, front, true, isDouble);

            parts.add(new SimpleModelWrapper(builder.build(), part.useAmbientOcclusion(), part.particleIcon()));
        }
    }

    private static void assemblePiece(
            Direction facing,
            BlockModelPart part,
            QuadCollection.Builder builder,
            boolean front,
            boolean topSlab,
            boolean isDouble
    ) {
        Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal());
        Vec3 normalScaled12 = normal.scale(12 / 16f);
        Vec3 normalScaledN8 = topSlab ? normal.scale((front ? 0 : -8) / 16f) : normal.scale((front ? 8 : 0) / 16f);
        float contract = 12;
        AABB bb = CUBE_AABB.contract(normal.x * contract / 16, normal.y * contract / 16, normal.z * contract / 16);
        if (!front)
            bb = bb.move(normalScaled12);

        processQuads(part.getQuads(null), facing, front, topSlab, isDouble, bb, normalScaledN8, builder::addUnculledFace);
        for (Direction direction : Iterate.directions) {
            Direction d = direction;
            processQuads(part.getQuads(direction), facing, front, topSlab, isDouble, bb, normalScaledN8, quad -> builder.addCulledFace(d, quad));
        }
    }

    private static void processQuads(
            List<BakedQuad> quads,
            Direction facing,
            boolean front,
            boolean topSlab,
            boolean isDouble,
            AABB bb,
            Vec3 move,
            Consumer<BakedQuad> consumer
    ) {
        for (BakedQuad quad : quads) {
            Direction direction = quad.direction();

            if (front && direction == facing)
                continue;
            if (!front && direction == facing.getOpposite())
                continue;
            if (isDouble && topSlab && direction == facing)
                continue;
            if (isDouble && !topSlab && direction == facing.getOpposite())
                continue;

            consumer.accept(BakedModelHelper.cropAndMove(quad, bb, move));
        }
    }
}
