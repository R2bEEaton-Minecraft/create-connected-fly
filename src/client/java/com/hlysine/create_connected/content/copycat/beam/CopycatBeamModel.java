package com.hlysine.create_connected.content.copycat.beam;

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
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Consumer;

import static net.minecraft.core.Direction.Axis;
import static net.minecraft.core.Direction.AxisDirection;

// Rewritten for MC 1.21.11's new "unbaked model parts" pipeline - see CopycatBlockModel's comment
// and PORTING_NOTES.md "CopycatModel architectural rewrite" for the general shape of this change.
public class CopycatBeamModel extends CopycatModel {
    protected static final AABB CUBE_AABB = new AABB(BlockPos.ZERO);

    public CopycatBeamModel(BlockState state, BlockStateModel.UnbakedRoot unbaked) {
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
        Axis axis = state.getOptionalValue(CopycatBeamBlock.AXIS).orElse(Axis.Y);

        Vec3 normal = Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE).getUnitVec3i());
        Vec3 rowNormal = axis.isVertical() ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 columnNormal = axis.isVertical() || axis == Axis.X ? new Vec3(0, 0, 1) : new Vec3(1, 0, 0);
        AABB bb = CUBE_AABB.contract((1 - normal.x) * 12 / 16, (1 - normal.y) * 12 / 16, (1 - normal.z) * 12 / 16);

        for (BlockModelPart part : getMaterialParts(world, pos, material, random, getModelOf(material))) {
            QuadCollection.Builder builder = new QuadCollection.Builder();

            // 4 Pieces
            for (boolean row : Iterate.trueAndFalse) {
                for (boolean column : Iterate.trueAndFalse) {
                    AABB bb1 = bb;
                    if (row)
                        bb1 = bb1.move(rowNormal.scale(12 / 16.0));
                    if (column)
                        bb1 = bb1.move(columnNormal.scale(12 / 16.0));

                    Vec3 offset = Vec3.ZERO;
                    Vec3 rowShift = rowNormal.scale(row ? -4 / 16.0 : 4 / 16.0);
                    Vec3 columnShift = columnNormal.scale(column ? -4 / 16.0 : 4 / 16.0);
                    offset = offset.add(rowShift);
                    offset = offset.add(columnShift);

                    rowShift = rowShift.normalize();
                    columnShift = columnShift.normalize();
                    Vec3i rowShiftNormal = new Vec3i((int) rowShift.x, (int) rowShift.y, (int) rowShift.z);
                    Vec3i columnShiftNormal = new Vec3i((int) columnShift.x, (int) columnShift.y, (int) columnShift.z);

                    AABB finalBb1 = bb1;
                    processQuads(part.getQuads(null), rowShiftNormal, columnShiftNormal, finalBb1, offset, builder::addUnculledFace);
                    for (Direction direction : Iterate.directions) {
                        Direction d = direction;
                        processQuads(part.getQuads(direction), rowShiftNormal, columnShiftNormal, finalBb1, offset, quad -> builder.addCulledFace(d, quad));
                    }
                }
            }

            parts.add(new SimpleModelWrapper(builder.build(), part.useAmbientOcclusion(), part.particleIcon()));
        }
    }

    private static void processQuads(
            List<BakedQuad> quads,
            Vec3i rowShiftNormal,
            Vec3i columnShiftNormal,
            AABB bb,
            Vec3 offset,
            Consumer<BakedQuad> consumer
    ) {
        for (BakedQuad quad : quads) {
            Direction direction = quad.direction();

            if (rowShiftNormal.equals(direction.getUnitVec3i()))
                continue;
            if (columnShiftNormal.equals(direction.getUnitVec3i()))
                continue;

            consumer.accept(BakedModelHelper.cropAndMove(quad, bb, offset));
        }
    }
}
