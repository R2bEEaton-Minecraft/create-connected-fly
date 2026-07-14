package com.hlysine.create_connected.content.fluidvessel;

import com.zurrtum.create.catnip.data.Iterate;
import com.zurrtum.create.client.AllSpriteShifts;
import com.zurrtum.create.client.foundation.block.connected.ConnectedTextureBehaviour;
import com.zurrtum.create.client.infrastructure.model.CTModel;
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

/** Current-model-pipeline replacement for the excluded NeoForge-era FluidVesselModel. */
public class FluidVesselFlyModel extends CTModel {
    private FluidVesselFlyModel(BlockState state, BlockStateModel.UnbakedRoot unbaked,
                                ConnectedTextureBehaviour behaviour) {
        super(state, unbaked, behaviour);
    }

    public static FluidVesselFlyModel standard(BlockState state, BlockStateModel.UnbakedRoot unbaked) {
        return new FluidVesselFlyModel(state, unbaked, new FluidVesselCTBehaviour(
                AllSpriteShifts.FLUID_TANK, AllSpriteShifts.FLUID_TANK_TOP, AllSpriteShifts.FLUID_TANK_INNER));
    }

    public static FluidVesselFlyModel creative(BlockState state, BlockStateModel.UnbakedRoot unbaked) {
        return new FluidVesselFlyModel(state, unbaked, new FluidVesselCTBehaviour(
                AllSpriteShifts.CREATIVE_FLUID_TANK, AllSpriteShifts.CREATIVE_CASING,
                AllSpriteShifts.CREATIVE_CASING));
    }

    @Override
    public void addPartsWithInfo(BlockAndTintGetter world, BlockPos pos, BlockState state,
                                 RandomSource random, List<BlockModelPart> parts) {
        int[] indices = createCTData(world, pos, state);
        Direction.Axis vesselAxis = state.getValue(FluidVesselBlock.AXIS);

        for (BlockModelPart part : model.collectParts(random)) {
            QuadCollection.Builder builder = new QuadCollection.Builder();
            part.getQuads(null).forEach(quad -> builder.addUnculledFace(
                    replaceQuad(state, random, indices[quad.direction().get3DDataValue()], quad)));

            for (Direction cullDirection : Iterate.directions) {
                boolean hideStructuralBoundary = cullDirection.getAxis() != vesselAxis
                        && FluidVesselBlock.isVessel(world.getBlockState(pos.relative(cullDirection)));

                // Fluid Vessel models reuse cullface as a "shared multiblock boundary" marker.
                // Some visible inner faces intentionally carry the opposite cullface, e.g. the
                // floor's upward-facing quad is tagged DOWN so it disappears only when another
                // vessel block occupies that boundary. If we emit those as genuinely culled DOWN
                // faces, any solid support block below also erases them.
                for (var quad : part.getQuads(cullDirection)) {
                    if (hideStructuralBoundary)
                        continue;

                    var replaced = replaceQuad(state, random,
                            indices[quad.direction().get3DDataValue()], quad);
                    if (quad.direction() == cullDirection) {
                        builder.addCulledFace(cullDirection, replaced);
                    } else {
                        builder.addUnculledFace(replaced);
                    }
                }
            }

            parts.add(new SimpleModelWrapper(builder.build(), part.useAmbientOcclusion(), part.particleIcon()));
        }
    }
}
